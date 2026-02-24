# Database Performance & Query Optimization

## 1. Detect Index Scan
When reviewing code that includes SQL queries or Hybris Flexible Search or ORM calls (like TypeORM, Prisma, SQLAlchemy, Hibernate):
- **Requirement**: Any new query must use `Index Seek` or `Index Scan` on a limited range. Full `Index Scan` or `Table Scan` on large tables is prohibited. Scan all *-items.xml files to check existing index for query and suggest for index table attribute
- **Detection**: Flag queries that use `SELECT *` without a `WHERE` clause on indexed columns.
- **Warning**: If a query filters by a column that is not part of an index define in files with Hybris items file with pattern `*-items.xml` (check schema definitions if available), suggest adding an index or refactoring.

## 2. Slow Response Patterns
Flag the following patterns as "Potential Slow Performance":
- **Leading Wildcards**: Using `LIKE '%keyword'` (causes full scan).
- **Functions in WHERE**: Using functions on indexed columns (e.g., `WHERE YEAR(created_at) = 2023`) which prevents index usage.
- **N+1 Queries**: Detecting loops that execute a database query inside each iteration.
- **Mismatched Data Types**: Comparing a string column with a numeric value (causes implicit conversion and ignores index).

## 3 JAVA RUNTIME EXCEPTION
Scan for **concrete, triggerable** runtime errors only:
- Null dereference (NPE)
- Unsafe cast
- `Optional.get()` without presence check
- Collection access without size/bound checks
- Illegal or invalid state in runtime flow

Flag issues only when a **real execution path** exists.  
Avoid speculative or “might happen” warnings.

## 4 MEMORY LEAK & MEMORY GROWTH
Scan for long-lived memory retention:
- Static references holding runtime objects
- Listeners or callbacks not released
- `ThreadLocal` not cleared
- Unbounded collections or caches without eviction
- Promotion context retaining large objects across executions

Flag only when objects outlive their expected lifecycle.  
Explain why GC cannot reclaim them and how to fix it.


## 5. Sample slow query Patterns
Query pattern that cause performance issue:
- ' FORMAT(T.created_at, 'yyyy-MM-dd') = :d '
- ' LOWER(T.email) = LOWER(:email) '
- ' CAST(T.order_id AS VARCHAR(50)) = :id
- ' SUBSTRING(CONVERT(VARCHAR(19), T.created_at, 120), 1, 10) = :d '
- ' p.catalogVersion = :cv OR p.approvalStatus = 'APPROVED' '
- ' p.code = :code OR p.code LIKE :prefix || '%' '
