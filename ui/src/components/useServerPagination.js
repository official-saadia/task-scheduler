import { useState, useEffect, useCallback } from 'react'
import { getErrorMessage } from '../api/client'

/**
 * Server-side pagination hook.
 *
 * Handles fetching paginated data from the backend, tracking loading/error state,
 * and exposing page controls. Works with Spring's Page<T> response format:
 * { content: [], totalPages, totalElements, number, size }
 *
 * @param {Function} fetchFn - function that accepts (page, size) and returns a Promise
 * @param {number} pageSize - items per page
 * @param {Array} deps - extra dependencies that trigger a fresh fetch from page 0 (e.g. a status filter)
 */
export function useServerPagination(fetchFn, pageSize = 10, deps = []) {
  const [data, setData] = useState([])
  const [currentPage, setCurrentPage] = useState(0) // Spring is 0-based
  const [totalPages, setTotalPages] = useState(0)
  const [totalItems, setTotalItems] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const fetch = useCallback(
    (page) => {
      setLoading(true)
      setError('')
      fetchFn(page, pageSize)
        .then((pageResult) => {
          setData(pageResult.content)
          setTotalPages(pageResult.totalPages)
          setTotalItems(pageResult.totalElements)
          setCurrentPage(pageResult.number)
        })
        .catch((err) => setError(getErrorMessage(err)))
        .finally(() => setLoading(false))
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [fetchFn, pageSize, ...deps]
  )

  // Reset to page 0 whenever deps change (e.g. filter changed)
  useEffect(() => {
    setCurrentPage(0)
    fetch(0)
  }, [fetch])

  function goToPage(page) {
    const target = Math.max(0, Math.min(page, totalPages - 1))
    setCurrentPage(target)
    fetch(target)
  }

  return {
    data,
    loading,
    error,
    currentPage,
    totalPages,
    totalItems,
    pageSize,
    // Convert to 1-based for display in Pagination component
    currentPageDisplay: currentPage + 1,
    totalPagesDisplay: totalPages,
    hasPrev: currentPage > 0,
    hasNext: currentPage < totalPages - 1,
    goToPage: (page) => goToPage(page - 1), // Pagination component is 1-based
    goToPrev: () => goToPage(currentPage - 1),
    goToNext: () => goToPage(currentPage + 1),
    refresh: () => fetch(currentPage),
  }
}
