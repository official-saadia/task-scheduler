import { useState, useMemo } from 'react'

/**
 * Client-side pagination hook.
 * Takes a full data array and returns the current page's slice plus controls.
 *
 * @param {Array} data - the full dataset to paginate
 * @param {number} pageSize - number of items per page (default 10)
 */
export function usePagination(data = [], pageSize = 10) {
  const [currentPage, setCurrentPage] = useState(1)

  const totalPages = Math.max(1, Math.ceil(data.length / pageSize))

  // Reset to page 1 whenever the data changes (e.g. after filter change)
  const safeCurrentPage = Math.min(currentPage, totalPages)

  const paginatedData = useMemo(() => {
    const start = (safeCurrentPage - 1) * pageSize
    return data.slice(start, start + pageSize)
  }, [data, safeCurrentPage, pageSize])

  function goToPage(page) {
    setCurrentPage(Math.max(1, Math.min(page, totalPages)))
  }

  return {
    paginatedData,
    currentPage: safeCurrentPage,
    totalPages,
    totalItems: data.length,
    pageSize,
    goToPage,
    goToPrev: () => goToPage(safeCurrentPage - 1),
    goToNext: () => goToPage(safeCurrentPage + 1),
    hasPrev: safeCurrentPage > 1,
    hasNext: safeCurrentPage < totalPages,
  }
}
