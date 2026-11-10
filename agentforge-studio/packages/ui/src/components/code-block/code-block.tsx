'use client';

import * as React from 'react';
import { codeToHtml } from 'shiki';
import { cn } from '../../lib/utils';

export interface CodeBlockProps extends React.HTMLAttributes<HTMLDivElement> {
  /**
   * 代码内容
   */
  code: string;
  /**
   * 编程语言
   */
  language?: string;
  /**
   * 主题名称
   * @default 'github-dark'
   */
  theme?: string;
  /**
   * 是否显示行号
   * @default false
   */
  showLineNumbers?: boolean;
  /**
   * 高亮的行号数组
   */
  highlightLines?: number[];
  /**
   * 是否显示复制按钮
   * @default true
   */
  showCopyButton?: boolean;
  /**
   * 文件名（可选）
   */
  filename?: string;
  /**
   * 最大高度
   */
  maxHeight?: string | number;
}

export const CodeBlock = React.forwardRef<HTMLDivElement, CodeBlockProps>(
  (
    {
      code,
      language = 'typescript',
      theme = 'github-dark',
      showLineNumbers = false,
      highlightLines = [],
      showCopyButton = true,
      filename,
      maxHeight,
      className,
      ...props
    },
    ref
  ) => {
    const [html, setHtml] = React.useState<string>('');
    const [copied, setCopied] = React.useState(false);
    const [isLoading, setIsLoading] = React.useState(true);

    React.useEffect(() => {
      let mounted = true;

      const highlight = async () => {
        try {
          setIsLoading(true);
          const result = await codeToHtml(code, {
            lang: language,
            theme: theme,
          });

          if (mounted) {
            setHtml(result);
            setIsLoading(false);
          }
        } catch (error) {
          console.error('Code highlighting failed:', error);
          if (mounted) {
            // 降级处理：显示纯文本
            setHtml(`<pre><code>${escapeHtml(code)}</code></pre>`);
            setIsLoading(false);
          }
        }
      };

      highlight();

      return () => {
        mounted = false;
      };
    }, [code, language, theme]);

    const handleCopy = async () => {
      try {
        await navigator.clipboard.writeText(code);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      } catch (error) {
        console.error('Failed to copy code:', error);
      }
    };

    return (
      <div
        ref={ref}
        className={cn(
          'group relative rounded-lg border border-outline-variant bg-surface',
          className
        )}
        {...props}
      >
        {/* 文件名栏 */}
        {filename && (
          <div className="flex items-center justify-between border-b border-outline-variant bg-surface-container-low/50 px-4 py-2">
            <span className="text-sm font-medium text-outline font-mono">
              {filename}
            </span>
          </div>
        )}

        {/* 代码区域 */}
        <div className="relative">
          {isLoading ? (
            <div className="flex items-center justify-center p-8">
              <div className="h-6 w-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
            </div>
          ) : (
            <div
              className={cn(
                'overflow-auto p-4',
                showLineNumbers && 'code-block-with-line-numbers'
              )}
              style={{
                maxHeight: maxHeight
                  ? typeof maxHeight === 'number'
                    ? `${maxHeight}px`
                    : maxHeight
                  : undefined,
              }}
              dangerouslySetInnerHTML={{ __html: html }}
            />
          )}

          {/* 复制按钮 */}
          {showCopyButton && !isLoading && (
            <button
              onClick={handleCopy}
              className={cn(
                'absolute right-2 top-2 rounded bg-surface/80 p-2',
                'opacity-0 transition-opacity group-hover:opacity-100',
                'hover:bg-background focus:opacity-100 focus:outline-none focus:ring-2 focus:ring-ring',
                'backdrop-blur-sm'
              )}
              aria-label={copied ? '已复制' : '复制代码'}
            >
              {copied ? (
                <CheckIcon className="h-4 w-4 text-green-500" />
              ) : (
                <CopyIcon className="h-4 w-4 text-outline" />
              )}
            </button>
          )}
        </div>
      </div>
    );
  }
);

CodeBlock.displayName = 'CodeBlock';

// 辅助函数：转义 HTML
function escapeHtml(text: string): string {
  const map: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;',
  };
  return text.replace(/[&<>"']/g, (m) => map[m] || m);
}

// 图标组件
function CopyIcon({ className }: { className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      <rect width="14" height="14" x="8" y="8" rx="2" ry="2" />
      <path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2" />
    </svg>
  );
}

function CheckIcon({ className }: { className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      <polyline points="20 6 9 17 4 12" />
    </svg>
  );
}
