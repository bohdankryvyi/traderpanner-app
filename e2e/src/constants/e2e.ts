/**
 * Shared E2E constants. Use these instead of magic strings for prefix segments and domains.
 */
export const E2E_PREFIX_BASE = 'E2E'
export const E2E_SEP = '|'

export const DOMAIN_PORTFOLIO = 'portfolio'
export const DOMAIN_TRADING = 'trading'

export const DOMAINS = [DOMAIN_PORTFOLIO, DOMAIN_TRADING] as const
export type E2EDomain = (typeof DOMAINS)[number]
