import React from 'react';
import katex from 'katex';
import 'katex/dist/katex.min.css';

/**
 * Renderiza una expresión LaTeX en HTML usando KaTeX de forma segura.
 */
function renderKatex(latex, displayMode = false) {
  try {
    return katex.renderToString(latex.trim(), {
      displayMode,
      throwOnError: false,
      strict: false,
      trust: true
    });
  } catch {
    return latex;
  }
}

/**
 * Procesa formato inline:
 * - Matemáticas en bloque $$...$$ o \[...\]
 * - Matemáticas inline $...$ o \(...\)
 * - Moneda escapada \$
 * - Negrita **...**
 * - Cursiva *...*
 * - Código inline `...`
 * - Enlaces Markdown [texto](url) y URLs directas https://...
 */
function parseInlineElements(text, isDarkBg = false) {
  if (!text) return null;

  // Normalizar moneda escapada \$ para que no se interprete como delimitador LaTeX
  const safeText = text.replace(/\\\$/g, '___ESCAPED_DOLLAR___');

  // Dividir primero por tokens matemáticos y de formato
  // Soporta: $$...$$, \[...\], \(...\), $...$ (cuando no es espacio al inicio/fin), **...**, `...`, [label](url), https://...
  const tokenRegex = /(\$\$[\s\S]+?\$\$|\\\[[\s\S]+?\\\]|\\\([\s\S]+?\\\)|\$(?!\s)(?:\\.|[^$\\])+(?<!\s)\$|\*\*[^*]+\*\*|`[^`]+`|\[[^\]]+\]\(https?:\/\/[^\s)]+\)|https?:\/\/[^\s)<]+|\*[^*\n]+\*)/g;

  const parts = safeText.split(tokenRegex);

  return parts.map((part, idx) => {
    if (!part) return null;

    const restored = part.replace(/___ESCAPED_DOLLAR___/g, '$');

    // 1. Bloque matemático $$...$$ o \[...\]
    if (
      (restored.startsWith('$$') && restored.endsWith('$$') && restored.length > 4) ||
      (restored.startsWith('\\[') && restored.endsWith('\\]') && restored.length > 4)
    ) {
      const formula = restored.slice(2, -2);
      const html = renderKatex(formula, true);
      return (
        <span
          key={idx}
          className="block my-2 overflow-x-auto"
          dangerouslySetInnerHTML={{ __html: html }}
        />
      );
    }

    // 2. Matemática inline \(...\)
    if (restored.startsWith('\\(') && restored.endsWith('\\)') && restored.length > 4) {
      const formula = restored.slice(2, -2);
      const html = renderKatex(formula, false);
      return (
        <span
          key={idx}
          className="inline-block align-middle mx-0.5"
          dangerouslySetInnerHTML={{ __html: html }}
        />
      );
    }

    // 3. Matemática inline $...$
    if (restored.startsWith('$') && restored.endsWith('$') && restored.length > 2) {
      const inner = restored.slice(1, -1);
      // Evitar falsos positivos si son dos montos en pesos en la misma línea sin comandos matemáticos
      const looksLikeCurrencyPair = /^\d[\d.,]*\s+.*\$\d/.test(inner);
      if (!looksLikeCurrencyPair) {
        const html = renderKatex(inner, false);
        return (
          <span
            key={idx}
            className="inline-block align-middle mx-0.5"
            dangerouslySetInnerHTML={{ __html: html }}
          />
        );
      }
    }

    // 4. Negrita **...**
    if (restored.startsWith('**') && restored.endsWith('**') && restored.length > 4) {
      const content = restored.slice(2, -2);
      return (
        <strong
          key={idx}
          className={`font-bold ${isDarkBg ? 'text-white' : 'text-gray-900'}`}
        >
          {parseInlineElements(content, isDarkBg)}
        </strong>
      );
    }

    // 5. Código inline `...`
    if (restored.startsWith('`') && restored.endsWith('`') && restored.length > 2) {
      return (
        <code
          key={idx}
          className={`px-1.5 py-0.5 rounded text-xs font-mono ${
            isDarkBg ? 'bg-white/20 text-white' : 'bg-gray-100 text-blue-800 border border-gray-200'
          }`}
        >
          {restored.slice(1, -1)}
        </code>
      );
    }

    // 6. Enlace Markdown [texto](url)
    const mdLinkMatch = restored.match(/^\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)$/);
    if (mdLinkMatch) {
      return (
        <a
          key={idx}
          href={mdLinkMatch[2]}
          target="_blank"
          rel="noopener noreferrer"
          className={`underline font-semibold break-all ${
            isDarkBg ? 'text-blue-200 hover:text-white' : 'text-blue-600 hover:text-blue-800'
          }`}
        >
          {mdLinkMatch[1]}
        </a>
      );
    }

    // 7. URL directa https://...
    if (/^https?:\/\/[^\s)<]+$/.test(restored)) {
      const cleanUrl = restored.replace(/[.,;:]+$/, '');
      const trailing = restored.slice(cleanUrl.length);
      return (
        <React.Fragment key={idx}>
          <a
            href={cleanUrl}
            target="_blank"
            rel="noopener noreferrer"
            className={`underline font-medium break-all ${
              isDarkBg ? 'text-blue-200 hover:text-white' : 'text-blue-600 hover:text-blue-800'
            }`}
          >
            {cleanUrl}
          </a>
          {trailing}
        </React.Fragment>
      );
    }

    // 8. Cursiva *...*
    if (restored.startsWith('*') && restored.endsWith('*') && restored.length > 2) {
      const content = restored.slice(1, -1);
      return (
        <em key={idx} className="italic">
          {parseInlineElements(content, isDarkBg)}
        </em>
      );
    }

    return <React.Fragment key={idx}>{restored}</React.Fragment>;
  });
}

/**
 * Componente que renderiza texto con Markdown enriquecido y fórmulas matemáticas LaTeX (KaTeX).
 */
export default function MathMarkdown({ content, isDarkBg = false, className = '' }) {
  if (!content) return null;

  const lines = String(content).split('\n');

  return (
    <div className={`space-y-1.5 leading-relaxed ${className}`}>
      {lines.map((rawLine, index) => {
        const line = rawLine.trimEnd();

        // Línea vacía -> espaciado sutil
        if (!line.trim()) {
          return <div key={index} className="h-1.5" />;
        }

        // Encabezados ###, ##, #
        const headingMatch = line.match(/^(#{1,4})\s+(.*)$/);
        if (headingMatch) {
          const level = headingMatch[1].length;
          const headingText = headingMatch[2];
          const sizeClass =
            level === 1
              ? 'text-base font-extrabold mt-2'
              : level === 2
              ? 'text-sm font-extrabold mt-2'
              : 'text-sm font-bold mt-1.5';
          return (
            <div
              key={index}
              className={`${sizeClass} ${isDarkBg ? 'text-white' : 'text-[#003B70]'}`}
            >
              {parseInlineElements(headingText, isDarkBg)}
            </div>
          );
        }

        // Viñetas (•, -, *)
        const bulletMatch = line.match(/^(\s*)([•\-*])\s+(.*)$/);
        if (bulletMatch) {
          const indent = bulletMatch[1].length > 0 ? 'pl-4' : 'pl-1';
          const itemText = bulletMatch[3];
          return (
            <div key={index} className={`flex items-start gap-2 ${indent}`}>
              <span
                className={`select-none font-bold mt-0.5 ${
                  isDarkBg ? 'text-blue-200' : 'text-blue-600'
                }`}
              >
                •
              </span>
              <div className="flex-1">{parseInlineElements(itemText, isDarkBg)}</div>
            </div>
          );
        }

        // Listas numeradas (1., 2., 1), a), etc.)
        const numberedMatch = line.match(/^(\s*)(\d+[.)]|[a-e][.)])\s+(.*)$/);
        if (numberedMatch) {
          const indent = numberedMatch[1].length > 0 ? 'pl-4' : '';
          const marker = numberedMatch[2];
          const itemText = numberedMatch[3];
          return (
            <div key={index} className={`flex items-start gap-2 ${indent}`}>
              <span
                className={`select-none font-bold min-w-[1.25rem] ${
                  isDarkBg ? 'text-blue-200' : 'text-[#003B70]'
                }`}
              >
                {marker}
              </span>
              <div className="flex-1">{parseInlineElements(itemText, isDarkBg)}</div>
            </div>
          );
        }

        // Párrafo normal
        return (
          <div key={index} className="break-words">
            {parseInlineElements(line, isDarkBg)}
          </div>
        );
      })}
    </div>
  );
}
