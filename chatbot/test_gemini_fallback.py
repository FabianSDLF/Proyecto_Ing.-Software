import os
import unittest
from unittest.mock import Mock, patch

from main import crear_interaccion_gemini


class GeminiFallbackTests(unittest.TestCase):
    @patch.dict(os.environ, {"GEMINI_MODELS": "primary,backup"})
    def test_transient_failure_preserves_request_on_backup(self):
        error = RuntimeError("temporarily unavailable")
        error.status_code = 503
        client = Mock()
        client.interactions.create.side_effect = [error, "response"]
        request = {"input": "hola", "previous_interaction_id": "conversation"}
        self.assertEqual(crear_interaccion_gemini(client, **request), "response")
        self.assertEqual(client.interactions.create.call_count, 2)
        client.interactions.create.assert_called_with(model="backup", **request)

    @patch.dict(os.environ, {"GEMINI_MODELS": "primary,backup"})
    def test_authentication_failure_does_not_try_other_models(self):
        error = RuntimeError("unauthorized")
        error.status_code = 401
        client = Mock()
        client.interactions.create.side_effect = error
        with self.assertRaises(RuntimeError):
            crear_interaccion_gemini(client, input="hola")
        self.assertEqual(client.interactions.create.call_count, 1)

    @patch.dict(os.environ, {"GEMINI_MODELS": "primary,backup"})
    def test_exhausted_models_propagate_failure(self):
        error = RuntimeError("temporarily unavailable")
        error.status_code = 503
        client = Mock()
        client.interactions.create.side_effect = error
        with self.assertRaises(RuntimeError):
            crear_interaccion_gemini(client, input="hola")
        self.assertEqual(client.interactions.create.call_count, 2)
