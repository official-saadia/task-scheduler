import { X } from 'lucide-react'

export default function Modal({ title, onClose, children, wide = false }) {
  return (
    <div className="fixed inset-0 z-50 flex items-start justify-end bg-black/40 backdrop-blur-sm">
      <div
        className={`h-full w-full ${wide ? 'max-w-2xl' : 'max-w-md'} overflow-y-auto bg-white border-l border-[color:var(--color-border-soft)] shadow-2xl`}
      >
        <div className="flex items-center justify-between border-b border-[color:var(--color-border-soft)] px-6 py-4 sticky top-0 bg-white z-10">
          <h2 className="text-lg font-semibold text-[color:var(--color-ink)]">{title}</h2>
          <button
            onClick={onClose}
            className="rounded-md p-1.5 text-[color:var(--color-ink-muted)] hover:bg-[color:var(--color-surface-raised)] hover:text-[color:var(--color-ink)] transition-colors"
            aria-label="Close"
          >
            <X size={18} />
          </button>
        </div>
        <div className="px-6 py-5">{children}</div>
      </div>
    </div>
  )
}
