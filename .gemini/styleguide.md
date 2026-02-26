# Database Performance & Java Code Quality

## MANDATORY RULES — READ FIRST

**These rules are NON-NEGOTIABLE. Skipping any of them is a review failure.**

1. You MUST produce a COMPLETE Index Verification Table for EVERY column in JOIN ON and WHERE clauses — not just the ones you suspect are missing indexes. A partial table is unacceptable.
2. You MUST explicitly check each item in the Completeness Checklist (Section 10) and include the filled checklist in your review output.
3. You MUST use the structured output format (Section 8) for every issue. No free-form paragraphs.
4. You MUST flag client-side aggregation that should be done in SQL as a separate issue with its own severity.
5. **ONE RULE PER REVIEW**: Each review comment MUST contain exactly ONE rule/issue. If a single code location violates multiple rules, you MUST create separate review comments for each rule. Do NOT combine multiple rules (e.g., SLOW-01 + PERF-03) into one review comment. Grouping multiple violations into a single comment is a review failure.

## 1. Detect Index Usage — Systematic Cross-Check

When reviewing code that includes SQL queries, Hybris FlexibleSearch, or ORM calls (TypeORM, Prisma, SQLAlchemy, Hibernate):

- **Requirement**: Any new query must use `Index Seek` or `Index Scan` on a limited range. Full `Index Scan` or `Table Scan` on large tables is prohibited.
- **Mandatory Step — Index Verification Table**: For **every** JOIN condition and **every** WHERE filter column in the query, produce a verification table. **You must list ALL columns, not just the problematic ones.** The table must be COMPLETE — a review that only flags 1 column out of 15+ is considered incomplete.

| Column | Table | Has Index? | Index Name | Source File |
|--------|-------|------------|------------|-------------|
| `{p.status}` | IS32Promotion | Yes | statusIdx | is32core-items.xml |
| `{p.redeemDigitalCoupon}` | IS32Promotion | **No** | — | is32core-items.xml |
| `{pt.elabPromotionDisplayType}` | IS32PromotionTag | ? | — | requires verification |
| ... | ... | ... | ... | ... |

  **You MUST list every single column** — not just the ones that are missing indexes. Scan **all** `*-items.xml` files in the repository to populate this table. For Hybris built-in types (e.g., `Coupon`, `Product`, `Customer`, `CouponRedemption`), note whether the join column is a known indexed attribute (e.g., `Product.code`, `Coupon.couponId`) or flag it as "requires verification — built-in type".

- **Detection**: Flag any column used in a JOIN ON or WHERE clause that does NOT appear in an index.
- **Composite Index Check**: When a WHERE clause filters on 2+ columns simultaneously (e.g., `status = ? AND suspended = ? AND startDate <= ? AND endDate > ?`), check whether a composite index covers the full filter combination. If only partial indexes exist, recommend a composite index covering the most selective column combination. **Explicitly state the recommended composite index column order** (most selective column first).
- **Both Sides of JOIN**: Always verify indexes on BOTH sides of a JOIN condition. A missing index on either side can cause a full scan on that table.

## 2. Slow Response Patterns

Flag the following patterns as "Potential Slow Performance". When flagging an issue, reference the Rule index (e.g., `SLOW-01`).

| Rule | Pattern |
|------|---------|
| SLOW-01 | Leading Wildcards: `LIKE '%keyword'` — causes full scan |
| SLOW-02 | Functions in WHERE on indexed columns (e.g., `WHERE YEAR(created_at) = 2023`) — prevents index usage |
| SLOW-03 | N+1 Queries: loop executing query per iteration, or DAO returning raw `List<List<Object>>` / `List<Object[]>` that callers re-query. Flag as N+1 risk; recommend aggregating in SQL or providing a higher-level method |
| SLOW-04 | Mismatched Data Types: string column compared with numeric value — implicit conversion ignores index |
| SLOW-05 | **[CRITICAL]** Unbounded Result Set: no `LIMIT` / pagination (`setMaxResults`, `setStart`) / row-count cap. Queries joining 3+ tables without pagination can cause OOM — **automatic CRITICAL, no exceptions** |
| SLOW-06 | Cartesian Product / JOIN Explosion: LEFT JOIN on 1:N without aggregation multiplies result set. **Quantify** the multiplication factor (e.g., "1000 CouponRedemptions × each row = 1000× explosion") |
| SLOW-07 | Large Intermediate Result Sets: join order doesn't reduce rows early, causing unnecessary data processing |
| SLOW-08 | **[HIGH]** Client-side Aggregation: caller aggregates in Java (GROUP BY, SUM, COUNT, DISTINCT) instead of SQL. Loading millions of rows into JVM heap wastes memory, CPU, and network I/O |
| SLOW-09 | OR-clause on different columns or mixed operators preventing single index scan. **Exception**: `(col IS NULL OR col <= ?date)` is a standard nullable-date guard — do NOT flag |

## 3. JAVA RUNTIME EXCEPTION

Scan every Java file for concrete runtime errors (NPE, unsafe cast, Optional.get, collection bounds, illegal state). Do NOT skip this section.

## 4. MEMORY LEAK & MEMORY GROWTH

Scan every Java file for memory retention issues (static refs, unclosed resources, unbounded collections, large result sets in heap). Do NOT skip this section.

## 5. WATCHED TABLES

**MANDATORY**: Cross-check EVERY query against this table. If a query touches any table listed below, you MUST apply extra scrutiny and produce a warning in the review output using the structured format (Section 8) with the Rule index. No exceptions.

**To add new rules**: append a row with a new `TABLE-nn` index.

| Rule | Table |
|------|-------|
| TABLE-01 | `is32loyaltytransaction` (~50M) |
| TABLE-02 | `is32loyaltytransaction` + `is32loyaltycard` |
| TABLE-03 | `is32fulfillmententry` (~20M) |
| TABLE-04 | `is32returnrequest` (~8M) |
| TABLE-05 | `is32returnrequest` + `is32fulfillmententry` |
| TABLE-06 | `is32loyaltycard` (~5M) |
| TABLE-07 | `is32warehouseallocation` (~2M) |

## 6. JAVA PERFORMANCE ANTI-PATTERNS

Flag the following patterns as performance concerns. When flagging, reference the Rule index (e.g., `PERF-01`). Do NOT skip this section.

| Rule | Pattern |
|------|---------|
| PERF-01 | String concatenation in loops instead of StringBuilder |
| PERF-02 | Unnecessary autoboxing/unboxing in hot paths or tight loops |
| PERF-03 | Inefficient collection choice for the access pattern used |
| PERF-04 | Object creation inside loops when reuse or pooling is possible |
| PERF-05 | Regex Pattern compiled inside loops or frequently called methods |
| PERF-06 | Inefficient Stream API usage where simple loops would perform better |
| PERF-07 | Redundant or repeated method calls that could be computed once |
| PERF-08 | Inefficient Map operations (e.g., containsKey + get instead of getOrDefault / putIfAbsent / computeIfAbsent) |
| PERF-09 | Unnecessary copying of collections or arrays |
| PERF-10 | Excessive synchronized blocks or lock contention in hot paths |

## 7. CONCURRENCY & THREAD SAFETY

Scan every Java file for concurrency issues and thread safety violations. Do NOT skip this section.

| Rule | Pattern |
|------|---------|
| CONC-01 | Shared mutable state without proper synchronization |
| CONC-02 | Non-thread-safe classes used in concurrent context (e.g., SimpleDateFormat, HashMap, ArrayList shared across threads) |
| CONC-03 | Double-checked locking without volatile |
| CONC-04 | Synchronization on non-final fields or mutable references |
| CONC-05 | Race conditions in check-then-act sequences |
| CONC-06 | Potential deadlock from inconsistent lock ordering |

## 8. REVIEW OUTPUT FORMAT

To ensure reviews are actionable, every issue MUST follow this exact format. **Do NOT use free-form paragraphs.** Every issue gets its own block:

```
### [SEVERITY: Critical/High/Medium] — Short title

**Rule**: ONE single rule index (e.g., SLOW-01 or PERF-03 or CONC-02)
**Location**: file:line or query line reference
**Issue**: Concrete description of what is wrong
**Evidence**: Reference to *-items.xml index definition, code line, or query pattern
**Impact**: What happens in production (e.g., "Full table scan on 10M-row Product table causing 30s response time under 100 concurrent users")
**Fix**: Specific actionable recommendation with code example if applicable
```

**Rules**:
- **ONE RULE PER REVIEW COMMENT**: Each review comment MUST reference exactly ONE rule. If the same code location violates multiple rules (e.g., SLOW-05 and PERF-04), create SEPARATE review comments — one for each rule. Never combine multiple rule indexes in a single review comment.
- Do NOT produce vague warnings like "this query may be slow" without specifying which join/filter is the problem, which index is missing, and what the fix is.
- **Impact must be quantified** where possible: estimate table sizes, row multiplication factors, or memory consumption. "Millions of rows" is better than "many rows". "500MB heap consumed loading 2M rows of 4 columns" is better than "high memory usage".
- **Fix must include code** for Critical and High issues. A textual description alone is insufficient.

## 9. SEVERITY CLASSIFICATION GUIDE

Use the following to determine severity. Do not downgrade severity for convenience.

| Severity | Criteria | Examples |
|----------|----------|---------|
| **CRITICAL** | Causes outage, data loss, or OOM in production | Unbounded result set on 8+ table join; missing index on JOIN column of a 10M+ row table; SQL injection |
| **HIGH** | Significant performance degradation or runtime crash | NPE on common code path; client-side aggregation of large result sets; Cartesian product; missing null-check on framework return value |
| **MEDIUM** | Suboptimal performance or code quality concern | PII logging; missing composite index for multi-column filter; query not cached when it could be; missing `@Transactional(readOnly=true)` |
| **LOW** | Minor improvement opportunity | Naming conventions; missing javadoc; unused import |

**Escalation rule**: If an issue combines two categories (e.g., unbounded result set + client-side aggregation), use the HIGHER severity.

## 10. REVIEW COMPLETENESS CHECKLIST

**MANDATORY**: You MUST include this filled checklist at the END of your review. Mark each item with [x] when completed. A review missing this checklist or with unchecked mandatory items will be considered incomplete.

Before submitting the review, verify ALL sections have been evaluated:

- [ ] Section 1: **COMPLETE** index verification table produced — every JOIN ON column and every WHERE column listed (not just flagged ones)
- [ ] Section 1: Composite index check performed for multi-column WHERE filters
- [ ] Section 1: Both sides of every JOIN verified for indexes
- [ ] Section 2: All slow patterns checked (SLOW-01 through SLOW-09)
- [ ] Section 3: Java runtime exceptions scanned (NPE, unsafe cast, Optional.get, collection bounds)
- [ ] Section 4: Memory issues scanned (unbounded collections, large result sets in heap, static references)
- [ ] Section 5: Every watched table cross-checked against the query (TABLE-01 through TABLE-07)
- [ ] Section 6: Java performance anti-patterns checked (PERF-01 through PERF-10)
- [ ] Section 7: Concurrency & thread safety issues scanned (CONC-01 through CONC-06)
- [ ] Section 8: Every issue follows the structured output format — ONE rule per review comment
- [ ] Verified: No review comment contains multiple rule indexes

