import unittest
from types import SimpleNamespace
from unittest.mock import Mock, patch
from fastapi.testclient import TestClient
import main


class ChatbotAccessTest(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(main.app)
        main.SESSION_INTERACTION_IDS.clear()

    def test_requests_without_session_are_rejected(self):
        response = self.client.post('/api/chat', json={'message': 'hola'})
        self.assertEqual(401, response.status_code)

    def test_api_key_is_not_configurable_by_student(self):
        with patch.object(main, 'authenticate_user', return_value={'id': 1, 'active': True, 'role': {'name': 'Estudiante'}}):
            response = self.client.post('/api/config/apikey', json={'api_key': 'test'})
        self.assertEqual(403, response.status_code)

    def test_identical_session_ids_do_not_share_history_between_users(self):
        def identity(header):
            return {'id': 1 if header == 'Bearer A' else 2, 'active': True}
        with patch.object(main, 'authenticate_user', side_effect=identity), \
             patch.object(main, 'get_gemini_client', return_value=Mock()), \
             patch.object(main, 'crear_interaccion_gemini', side_effect=[
                 SimpleNamespace(id='A1', output_text='a'), SimpleNamespace(id='B1', output_text='b'),
                 SimpleNamespace(id='A2', output_text='a2')]) as generate:
            for token in ['A', 'B', 'A']:
                response = self.client.post('/api/chat', headers={'Authorization': 'Bearer ' + token},
                                            json={'session_id': 'same', 'message': 'hola'})
                self.assertEqual(200, response.status_code)
            self.assertNotIn('previous_interaction_id', generate.call_args_list[0].kwargs)
            self.assertNotIn('previous_interaction_id', generate.call_args_list[1].kwargs)
            self.assertEqual('A1', generate.call_args_list[2].kwargs['previous_interaction_id'])

    def test_backend_unavailable_fails_closed(self):
        with patch.object(main.urllib.request, 'urlopen', side_effect=main.urllib.error.URLError('offline')):
            response = self.client.post('/api/chat', headers={'Authorization': 'Bearer invalid'}, json={'message': 'hola'})
        self.assertEqual(503, response.status_code)


if __name__ == '__main__':
    unittest.main()
