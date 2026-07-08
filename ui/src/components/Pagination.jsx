import { ChevronLeft, ChevronRight } from 'lucide-react'

/**
 * Pagination bar used at the bottom of every listing page.
 * Renders prev/next buttons, page number buttons, and a total items count.
 */
export default function Pagination({
  currentPage,
  totalPages,
  totalItems,
  pageSize,
  goToPage,
  goToPrev,
  goToNext,
  hasPrev,
  hasNext,
}) {
  if (totalItems === 0) return null

  const start = (currentPage - 1) * pageSize + 1
  const end = Math.min(currentPage * pageSize, totalItems)

  // Build page number array — show max 5 pages with ellipsis
  const pages = buildPageNumbers(currentPage, totalPages)

  return (
    <div className="mt-4 flex items-center justify-between text-sm">
      <p className="text-[color:var(--color-ink-muted)] font-[var(--font-mono)] text-xs">
        {start}–{end} of {totalItems}
      </p>

      <div className="flex items-center gap-1">
        <button
          onClick={goToPrev}
          disabled={!hasPrev}
          className="rounded-md p-1.5 text-[color:var(--color-ink-muted)] hover:bg-[color:var(--color-surface-raised)] hover:text-[color:var(--color-ink)] disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
          aria-label="Previous page"
        >
          <ChevronLeft size={16} />
        </button>

        {pages.map((p, i) =>
          p === '...' ? (
            <span
              key={`ellipsis-${i}`}
              className="px-2 text-[color:var(--color-ink-faint)]"
            >
              …
            </span>
          ) : (
            <button
              key={p}
              onClick={() => goToPage(p)}
              className={`min-w-[32px] rounded-md px-2 py-1 text-xs font-medium transition-colors ${
                p === currentPage
                  ? 'bg-[color:var(--color-accent)] text-white'
                  : 'text-[color:var(--color-ink-muted)] hover:bg-[color:var(--color-surface-raised)] hover:text-[color:var(--color-ink)]'
              }`}
            >
              {p}
            </button>
          )
        )}

        <button
          onClick={goToNext}
          disabled={!hasNext}
          className="rounded-md p-1.5 text-[color:var(--color-ink-muted)] hover:bg-[color:var(--color-surface-raised)] hover:text-[color:var(--color-ink)] disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
          aria-label="Next page"
        >
          <ChevronRight size={16} />
        </button>
      </div>
    </div>
  )
}

/**
 * Generates a page number array with ellipsis for large page counts.
 * e.g. [1, '...', 4, 5, 6, '...', 20]
 */
function buildPageNumbers(current, total) {
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }

  const pages = []

  pages.push(1)

  if (current > 3) pages.push('...')

  const rangeStart = Math.max(2, current - 1)
  const rangeEnd = Math.min(total - 1, current + 1)

  for (let i = rangeStart; i <= rangeEnd; i++) {
    pages.push(i)
  }

  if (current < total - 2) pages.push('...')

  pages.push(total)

  return pages
}
