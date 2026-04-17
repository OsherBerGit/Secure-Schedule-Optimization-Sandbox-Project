/**
 * Strips ugly algorithm-internal prefixes from reason strings while preserving
 * the detailed message (worker IDs, days, times) from the backend.
 *
 * Examples:
 *   "Memetic: constraint violation during decode - Worker 3 unavailable on MONDAY"
 *     → "Worker 3 unavailable on MONDAY"
 *   "Greedy: no eligible worker found for task"
 *     → "No eligible worker found for task"
 *   "Worker shift conflict: availability window 09:00–17:00 does not cover task"
 *     → "Worker shift conflict: availability window 09:00–17:00 does not cover task"
 */
export function formatReason(rawReason: string): string {
    if (!rawReason) return 'Unknown reason'

    const prefixPattern = /^(memetic|greedy|round.?robin)\s*:\s*(constraint violation during decode\s*[-–-]+\s*|[^-–]*(decode|scheduling)\s*[-–-]+\s*)?/i
    const stripped = rawReason.replace(prefixPattern, '').trim()

    const result = stripped.length > 0 ? stripped : rawReason.trim()

    // Capitalise the first letter only
    return result.charAt(0).toUpperCase() + result.slice(1)
}

export function getPriorityColor(priorityName: string | null) {
    switch (priorityName?.toUpperCase()) {
        case 'HIGH':     return '#e74c3c'
        case 'MEDIUM':   return '#f39c12'
        case 'LOW':      return '#27ae60'
        default:         return '#667eea'
    }
}
