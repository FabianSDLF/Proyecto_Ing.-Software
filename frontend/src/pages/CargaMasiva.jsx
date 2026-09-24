import { useState } from 'react';
import toast from 'react-hot-toast';
import userService from '../services/userService';
import { Button } from '../components/ui';

export default function CargaMasiva() {
    const [file, setFile] = useState(null);
    const [loading, setLoading] = useState(false);

    const handleUpload = async (e) => {
        e.preventDefault();
        if (!file) {
            toast.error("Por favor, selecciona un archivo CSV o Excel.");
            return;
        }

        const loadingToast = toast.loading("Procesando usuarios...");
        setLoading(true);

        try {
            await userService.uploadBatch(file);
            toast.dismiss(loadingToast);
            toast.success("¡Usuarios cargados con éxito!");
            setFile(null);
            // Reset input element
            e.target.reset();
        } catch (err) {
            toast.dismiss(loadingToast);
            toast.error(err.message || "Error al procesar el archivo masivo.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="p-8 max-w-4xl mx-auto space-y-8">
            <h1 className="text-4xl font-extrabold text-blue-900 tracking-tight">Carga Masiva</h1>

            <div className="bg-white p-8 rounded-2xl shadow-lg border border-gray-100">
                <div className="flex items-center mb-6">
                    <div className="bg-blue-100 p-3 rounded-full mr-4">
                        <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path></svg>
                    </div>
                    <div>
                        <h2 className="text-2xl font-bold text-gray-800">Importar Usuarios</h2>
                        <p className="text-gray-500 mt-1">Sube un archivo .csv o .xlsx para registrar alumnos y profesores automáticamente.</p>
                    </div>
                </div>

                <form onSubmit={handleUpload} className="flex flex-col space-y-5 mt-4">
                    <div className="border-2 border-dashed border-gray-300 rounded-xl p-6 text-center hover:bg-gray-50 transition-colors">
                        <input
                            type="file"
                            accept=".csv, .xlsx"
                            onChange={(e) => setFile(e.target.files?.[0] || null)}
                            className="w-full text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100 cursor-pointer"
                        />
                    </div>
                    <Button
                        type="submit"
                        variant="primary"
                        size="lg"
                        className="w-full bg-orange-600 hover:bg-orange-700 font-bold"
                        isLoading={loading}
                    >
                        Procesar Usuarios
                    </Button>
                </form>
            </div>
        </div>
    );
}
