# Database Performance & Query Optimization

## 1. Detect Index Usage — Systematic Cross-Check

When reviewing code that includes SQL queries, Hybris FlexibleSearch, or ORM calls (TypeORM, Prisma, SQLAlchemy, Hibernate):

- **Requirement**: Any new query must use `Index Seek` or `Index Scan` on a limited range. Full `Index Scan` or `Table Scan` on large tables is prohibited.
- **Mandatory Step — Index Verification Table**: For every JOIN condition and every WHERE filter column in the query, produce a verification table in the following format:

| Column | Table | Has Index? | Index Name | Source File |
|--------|-------|------------|------------|-------------|
| `{p.status}` | IS32Promotion | Yes | statusIdx | is32core-items.xml |
| `{p.redeemDigitalCoupon}` | IS32Promotion | **No** | — | is32core-items.xml |

  Scan **all** `*-items.xml` files in the repository to populate this table. For Hybris built-in types (e.g., `Coupon`, `Product`, `Customer`, `CouponRedemption`), note whether the join column is a known indexed attribute (e.g., `Product.code`, `Coupon.couponId`) or flag it as "requires verification — built-in type".

- **Detection**: Flag any column used in a JOIN ON or WHERE clause that does NOT appear in an index.
- **Composite Index Check**: When a WHERE clause filters on 2+ columns simultaneously (e.g., `status = ? AND suspended = ? AND startDate <= ? AND endDate > ?`), check whether a composite index covers the full filter combination. If only partial indexes exist, recommend a composite index covering the most selective column combination.
- **Both Sides of JOIN**: Always verify indexes on BOTH sides of a JOIN condition. A missing index on either side can cause a full scan on that table.

## 2. Slow Response Patterns

Flag the following patterns as "Potential Slow Performance":

- **Leading Wildcards**: Using `LIKE '%keyword'` (causes full scan).
- **Functions in WHERE**: Using functions on indexed columns (e.g., `WHERE YEAR(created_at) = 2023`) which prevents index usage.
- **N+1 Queries**: Detecting loops that execute a database query inside each iteration. Also detect DAO methods returning raw data that callers are likely to iterate and re-query.
- **Mismatched Data Types**: Comparing a string column with a numeric value (causes implicit conversion and ignores index).
- **Unbounded Result Set (CRITICAL)**: Flag any query that does NOT have a `LIMIT`, pagination (`setMaxResults`, `setStart`), or row-count cap. Queries joining 3+ tables without pagination can produce millions of rows and cause OOM.
- **Cartesian Product / JOIN Explosion**: When using LEFT JOIN, flag if the join can multiply the result set (e.g., a LEFT JOIN on a one-to-many relationship without aggregation). Especially dangerous when multiple LEFT JOINs are chained.
- **Large Intermediate Result Sets**: When a query joins many tables, verify that the join order and filters reduce row counts early. JOINs should be ordered so that the most restrictive filters apply first.
- **IS NULL OR pattern distinction**: The pattern `(col IS NULL OR col <= ?date)` is a standard nullable-date guard and is generally NOT a performance concern — do NOT flag this as an OR-clause issue. Only flag OR clauses where both branches reference different columns or use different operators that prevent a single index scan, such as `p.code = :code OR p.code LIKE :prefix || '%'`.

## 3. JAVA RUNTIME EXCEPTION

Scan for **concrete, triggerable** runtime errors only:

- **Null dereference (NPE)**:
  - Field injected via setter without null-check at usage point (e.g., `flexibleSearchService` set via `setFlexibleSearchService()` but used directly without guard)
  - Method parameters not validated before dereferencing (e.g., `customer.getPk()` called without checking `customer != null`)
  - Return values from framework calls that may return null (e.g., `SearchResult.getResult()` may return null — check for null before calling `.isEmpty()`)
- **Unsafe cast**
- **`Optional.get()` without presence check**
- **Collection access without size/bound checks**
- **Illegal or invalid state in runtime flow**

Flag issues only when a **real execution path** exists.
Avoid speculative or "might happen" warnings.

**Important**: Do NOT skip this section even when the primary focus of the review is on query performance. Every Java file must be scanned for runtime exceptions.

## 4. MEMORY LEAK & MEMORY GROWTH

Scan for long-lived memory retention:

- **Static references holding runtime objects**
- **Listeners or callbacks not released**
- **`ThreadLocal` not cleared**
- **Unbounded collections or caches without eviction**
- **Promotion context retaining large objects across executions**
- **Large query results loaded entirely into heap**: Methods returning `List<List<Object>>` or `List<Model>` from multi-table JOINs without pagination. If the result set is unbounded, this is effectively an OOM risk under production load. Recommend streaming (`ScrollableResults`), batching, or adding `setMaxResults()`.

Flag only when objects outlive their expected lifecycle.
Explain why GC cannot reclaim them and how to fix it.

**Important**: Do NOT skip this section even when the primary focus of the review is on query performance. Every Java file must be scanned for memory issues.

## 5. Sample Slow Query Patterns

Query patterns that cause performance issues:

- `FORMAT(T.created_at, 'yyyy-MM-dd') = :d` — function on indexed column
- `LOWER(T.email) = LOWER(:email)` — function on indexed column
- `CAST(T.order_id AS VARCHAR(50)) = :id` — implicit type conversion
- `SUBSTRING(CONVERT(VARCHAR(19), T.created_at, 120), 1, 10) = :d` — nested functions
- `p.catalogVersion = :cv OR p.approvalStatus = 'APPROVED'` — OR across different columns prevents single index usage
- `p.code = :code OR p.code LIKE :prefix || '%'` — OR with LIKE prevents index seek

## 6. MULTI-TABLE JOIN REVIEW PROTOCOL

When a query joins 4 or more tables, apply the following additional checks:

- **Join Count Threshold**: Queries joining 6+ tables require an explicit justification in the review. Suggest breaking into smaller queries or using a materialized/cached view if the join count exceeds 8.
- **Join Type Analysis**: For each JOIN, state whether it is INNER or LEFT and whether it can multiply rows (one-to-many). Chain of LEFT JOINs on one-to-many relationships is a red flag for result set explosion.
- **Filter Pushdown**: Verify that WHERE conditions are applied to the driving table (first table in FROM) to reduce the initial scan early. Filters that only apply to the last-joined table force the database to scan and join everything first.
- **SELECT Column Analysis**: Flag `SELECT *` or selecting columns from all joined tables when only a subset is needed. Unnecessary columns increase I/O and memory usage.
- **Missing Aggregation**: If the query returns denormalized rows that the Java code must aggregate (e.g., GROUP BY in comments but not in query), recommend moving aggregation to the database.

## 7. REVIEW OUTPUT FORMAT

To ensure reviews are actionable, every issue must follow this format:

```
### [SEVERITY: Critical/High/Medium] — Short title

**Location**: file:line or query line reference
**Issue**: Concrete description of what is wrong
**Evidence**: Reference to *-items.xml index definition, code line, or query pattern
**Impact**: What happens in production (e.g., "Full table scan on 10M-row Product table")
**Fix**: Specific actionable recommendation with code example if applicable
```

Do NOT produce vague warnings like "this query may be slow" without specifying which join/filter is the problem, which index is missing, and what the fix is.

## 8. REVIEW COMPLETENESS CHECKLIST

Before submitting the review, verify ALL sections have been evaluated:

- [ ] Section 1: Index verification table produced for every JOIN and WHERE column
- [ ] Section 2: All slow patterns checked, including unbounded result set
- [ ] Section 3: Java runtime exceptions scanned (NPE, unsafe cast, etc.)
- [ ] Section 4: Memory issues scanned (unbounded collections, large result sets in heap)
- [ ] Section 5: Query patterns cross-checked against sample slow patterns
- [ ] Section 6: Multi-table join protocol applied (if 4+ tables)
- [ ] Section 7: Every issue follows the structured output format
