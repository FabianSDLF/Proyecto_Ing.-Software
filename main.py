import os
import glob
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import google.generativeai as genai

# Librerías de LangChain
from langchain_community.document_loaders import PyPDFLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_community.vectorstores import FAISS
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain.chains import create_history_aware_retriever, create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_community.chat_message_histories import ChatMessageHistory
from langchain_core.runnables.history import RunnableWithMessageHistory
from langchain_core.prompts import PromptTemplate

# Variables globales
RUTA_CARPETA = './recursos'
store = {}
agente_chain = None

def configurar_api():
    api_key = os.environ.get("GOOGLE_API_KEY")
    if not api_key:
        print("⚠️ Advertencia: No se encontró GOOGLE_API_KEY en las variables de entorno.")
        return False
    genai.configure(api_key=api_key)
    return True

def crear_vectorstore(folder_path):
    print(f"🚀 Procesando carpeta de recursos: {folder_path}")
    if not os.path.exists(folder_path):
        os.makedirs(folder_path)
        print(f"⚠️ La carpeta {folder_path} fue creada vacía. Añade PDFs ahí.")
        return None

    pdf_files = glob.glob(f"{folder_path}/*.pdf")
    if not pdf_files:
        print(f"⚠️ Advertencia: No se encontraron archivos PDF en {folder_path}.")
        return None

    documents = []
    for file in pdf_files:
        documents.extend(PyPDFLoader(file).load())

    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=1000, chunk_overlap=200, separators=["\n\n", "\n", " ", ""]
    )
    splits = text_splitter.split_documents(documents)

    if not splits:
        print("⚠️ Advertencia: No se pudo extraer texto de los documentos.")
        return None

    embeddings = HuggingFaceEmbeddings(model_name="sentence-transformers/all-MiniLM-L6-v2")
    vectorstore = FAISS.from_documents(splits, embeddings)
    print(f"✅ Vectorstore creado con {len(splits)} fragmentos.")
    return vectorstore

def get_session_history(session_id: str):
    if session_id not in store:
        store[session_id] = ChatMessageHistory()
    return store[session_id]

def inicializar_rag_chain(vectorstore):
    if vectorstore is None:
        print("ℹ️ Iniciando cadena SIN RAG (Modo chat normal) porque no hay vectorstore.")
        # Podríamos inicializar un bot genérico si queremos, pero para mantener la estructura original
        # simplemente requerimos el vectorstore o usamos un fallback.
        # Por simplicidad, retornaremos None o un bot genérico.
    
    llm = ChatGoogleGenerativeAI(model="gemini-1.5-flash", temperature=0) # Usando 1.5-flash ya que 3.8-flash fue un typo común o modelo futuro
    
    if vectorstore:
        retriever = vectorstore.as_retriever(search_kwargs={"k": 5})
        contextualize_q_system_prompt = (
            "Dado el historial del chat y la última pregunta del usuario "
            "la cual podría hacer referencia al contexto del historial, "
            "formula una pregunta independiente que pueda entenderse "
            "sin el historial del chat. NO respondas la pregunta, "
            "simplemente reformúlala si es necesario y de lo contrario devuélvela tal cual."
        )
        qa_system_prompt = (
            "Eres un asistente para tareas de preguntas y respuestas. "
            "Usa los siguientes fragmentos de contexto recuperado para responder la pregunta. "
            "Si no sabes la respuesta, simplemente di que no lo sabes. "
            "Usa un máximo de tres oraciones y mantén la respuesta concisa. "
            "Responde SIEMPRE en español.\n\n"
            "Contexto: {context}"
        )

        history_prompt = ChatPromptTemplate.from_messages([
            ("system", contextualize_q_system_prompt),
            MessagesPlaceholder("chat_history"),
            ("human", "{input}"),
        ])

        qa_prompt = ChatPromptTemplate.from_messages([
            ("system", qa_system_prompt),
            MessagesPlaceholder("chat_history"),
            ("human", "{input}"),
        ])

        document_prompt = PromptTemplate(
            input_variables=["page_content", "source"],
            template="Contenido: {page_content}\nFuente: {source}"
        )

        history_aware_retriever = create_history_aware_retriever(llm, retriever, history_prompt)
        question_answer_chain = create_stuff_documents_chain(llm, qa_prompt, document_prompt=document_prompt)
        rag_chain = create_retrieval_chain(history_aware_retriever, question_answer_chain)
        
        conversational_rag_chain = RunnableWithMessageHistory(
            rag_chain,
            get_session_history,
            input_messages_key="input",
            history_messages_key="chat_history",
            output_messages_key="answer",
        )
        return conversational_rag_chain, True # is_rag=True
    else:
        # Fallback sin RAG si no hay PDFs
        qa_prompt = ChatPromptTemplate.from_messages([
            ("system", "Eres un asistente inteligente para estudiantes. Responde en español y sé conciso."),
            MessagesPlaceholder("chat_history"),
            ("human", "{input}"),
        ])
        chain = qa_prompt | llm
        conversational_chain = RunnableWithMessageHistory(
            chain,
            get_session_history,
            input_messages_key="input",
            history_messages_key="chat_history",
        )
        return conversational_chain, False # is_rag=False


@asynccontextmanager
async def lifespan(app: FastAPI):
    global agente_chain, is_rag
    configurar_api()
    vs = crear_vectorstore(RUTA_CARPETA)
    agente_chain, is_rag = inicializar_rag_chain(vs)
    yield
    store.clear()

app = FastAPI(lifespan=lifespan)

# Configurar CORS para permitir que React (puerto 80) acceda
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class ChatRequest(BaseModel):
    session_id: str
    message: str

@app.post("/chat")
async def chat_endpoint(request: ChatRequest):
    if not agente_chain:
        raise HTTPException(status_code=500, detail="El agente no está inicializado.")
    
    try:
        if is_rag:
            response = agente_chain.invoke(
                {"input": request.message},
                config={"configurable": {"session_id": request.session_id}}
            )
            return {"answer": response["answer"]}
        else:
            response = agente_chain.invoke(
                {"input": request.message},
                config={"configurable": {"session_id": request.session_id}}
            )
            return {"answer": response.content} # Langchain fallback returns AIMessage
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
