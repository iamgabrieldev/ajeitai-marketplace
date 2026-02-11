"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useAuth } from "@/providers/auth-provider";
import { documentosApi, type Documento } from "@/lib/api";
import { PageHeader } from "@/components/layout/page-header";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Upload,
  FileText,
  Download,
  Trash2,
  File,
  FolderOpen,
} from "lucide-react";

export default function DocumentosPage() {
  const { token } = useAuth();
  const fileRef = useRef<HTMLInputElement>(null);
  const [documentos, setDocumentos] = useState<Documento[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);

  const fetchDocumentos = useCallback(async () => {
    if (!token) return;
    try {
      const data = await documentosApi.listar(token);
      setDocumentos(data);
    } catch (err) {
      console.error("Erro ao buscar documentos:", err);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchDocumentos();
  }, [fetchDocumentos]);

  const handleUpload = async (file: File) => {
    if (!token) return;
    setUploading(true);
    try {
      await documentosApi.upload(token, file);
      fetchDocumentos();
    } catch (err) {
      console.error("Erro ao enviar documento:", err);
    } finally {
      setUploading(false);
    }
  };

  const handleExcluir = async (id: string) => {
    if (!token) return;
    try {
      await documentosApi.excluir(token, id);
      fetchDocumentos();
    } catch (err) {
      console.error("Erro ao excluir:", err);
    }
  };

  const handleDownload = (id: string) => {
    if (!token) return;
    const url = documentosApi.download(token, id);
    window.open(url, "_blank");
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString("pt-BR", {
      day: "2-digit",
      month: "short",
      year: "numeric",
    });
  };

  const getFileIcon = (tipo: string) => {
    if (tipo.includes("pdf")) return "PDF";
    if (tipo.includes("image")) return "IMG";
    return "DOC";
  };

  return (
    <div className="flex flex-col">
      <PageHeader
        title="Documentos"
        subtitle="Gerencie seus documentos profissionais"
      />

      <div className="px-4 py-4">
        {/* Upload */}
        <Card className="mb-4">
          <div className="flex flex-col items-center gap-3 py-4">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-primary/10">
              <Upload className="h-7 w-7 text-primary" />
            </div>
            <div className="text-center">
              <p className="font-medium text-text dark:text-text-dark">
                Enviar documento
              </p>
              <p className="text-sm text-text-muted dark:text-text-dark-muted">
                PDF, imagem ou documento (máx. 10MB)
              </p>
            </div>
            <Button
              variant="outline"
              onClick={() => fileRef.current?.click()}
              loading={uploading}
              icon={<FileText className="h-4 w-4" />}
            >
              Escolher arquivo
            </Button>
            <input
              ref={fileRef}
              type="file"
              accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
              className="hidden"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) handleUpload(file);
              }}
            />
          </div>
        </Card>

        {/* Lista */}
        {loading ? (
          <div className="space-y-3">
            {[1, 2].map((i) => (
              <Card key={i} className="animate-pulse">
                <div className="flex items-center gap-3">
                  <div className="h-12 w-12 rounded-xl bg-secondary-200 dark:bg-secondary-700" />
                  <div className="flex-1 space-y-2">
                    <div className="h-4 w-3/4 rounded bg-secondary-200 dark:bg-secondary-700" />
                    <div className="h-3 w-1/2 rounded bg-secondary-200 dark:bg-secondary-700" />
                  </div>
                </div>
              </Card>
            ))}
          </div>
        ) : documentos.length === 0 ? (
          <Card className="py-12 text-center">
            <FolderOpen className="mx-auto mb-3 h-12 w-12 text-secondary-300 dark:text-secondary-600" />
            <p className="text-text-muted dark:text-text-dark-muted">
              Nenhum documento enviado.
            </p>
          </Card>
        ) : (
          <div className="flex flex-col gap-3">
            {documentos.map((doc) => (
              <Card key={doc.id}>
                <div className="flex items-center gap-3">
                  <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-primary/10">
                    <File className="h-6 w-6 text-primary" />
                    <span className="sr-only">{getFileIcon(doc.tipo)}</span>
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-medium text-text dark:text-text-dark">
                      {doc.nome}
                    </p>
                    <p className="text-xs text-text-muted dark:text-text-dark-muted">
                      {formatDate(doc.createdAt)}
                    </p>
                  </div>
                  <div className="flex gap-1">
                    <button
                      onClick={() => handleDownload(doc.id)}
                      className="rounded-lg p-2 text-primary hover:bg-primary-50 dark:hover:bg-primary-900/20"
                      aria-label="Download"
                    >
                      <Download className="h-5 w-5" />
                    </button>
                    <button
                      onClick={() => handleExcluir(doc.id)}
                      className="rounded-lg p-2 text-danger hover:bg-red-50 dark:hover:bg-red-900/20"
                      aria-label="Excluir"
                    >
                      <Trash2 className="h-5 w-5" />
                    </button>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
