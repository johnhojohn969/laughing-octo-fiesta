# Database Performance & Java Code Quality

## MANDATORY RULES — READ FIRST

**These rules are NON-NEGOTIABLE. Skipping any of them is a review failure.**

1. You MUST produce a COMPLETE Index Verification Table for EVERY column in JOIN ON and WHERE clauses — not just the ones you suspect are missing indexes. A partial table is unacceptable.
2. You MUST explicitly check each item in the Completeness Checklist (Section 9) and include the filled checklist in your review output.
3. You MUST use the structured output format (Section 7) for every issue. No free-form paragraphs.
4. You MUST flag client-side aggregation that should be done in SQL as a separate issue with its own severity.
5. **ONE ISSUE PER REVIEW COMMENT — NEVER GROUP**: Each review comment MUST address exactly ONE single issue from ONE single section. NEVER merge findings from different sections into one comment (e.g., do NOT combine a SLOW-xx issue with a runtime exception or a memory issue in the same comment). NEVER list multiple issues from the same section in one comment either. If you find 5 issues, you MUST produce 5 separate review comments. Grouping is a review failure.

6. You MUST perform a **QUERY ANATOMY DECOMPOSITION** for EVERY FlexibleSearch or SQL query found. Before cross-checking against pattern tables, you MUST explicitly decompose the query into its structural components and reason about each one independently. See Section 5.2 for the mandatory decomposition protocol.
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

**MANDATORY**: Cross-check EVERY query against this table. If a query touches any table listed below, you MUST apply extra scrutiny and produce a warning in the review output using the structured format (Section 7) with the Rule index. No exceptions.

**To add new rules**: append a row with a new `TABLE-nn` index.

| Rule | Table |
|------|-------|
| TABLE-01 | `is32loyaltytransaction` |
| TABLE-03 | `is32fulfillmententry`|
| TABLE-04 | `is32returnrequest`|
| TABLE-06 | `is32loyaltycard`|
| TABLE-07 | `is32warehouseallocation`|

### 5.1 FLEXIBLESEARCH QUERY PATTERN DETECTION

**MANDATORY**: When reviewing Java code that contains FlexibleSearch queries (strings passed to `flexibleSearchService.search()`, `FlexibleSearchQuery`, or any query string that uses Hybris FlexibleSearch syntax like `SELECT ... FROM {TypeName}` or `{p:attribute}`), you MUST cross-check against the patterns below. If a FlexibleSearch query structurally matches or resembles any of these patterns, flag it using the corresponding `SQL-nn` rule index.

**To add new rules**: append a row with a new `SQL-nn` index.

| Rule | Pattern |
|------|---------|
| SQL-01 | SELECT item_t0.PK FROM is32promotion item_t0 JOIN is32bucket item_t1 ON item_t1.p_promotionuid = item_t0.p_uid WHERE ( item_t0.p_redeemdigitalcoupon IS NOT NULL AND item_t0.p_requiredcoupon = '' AND item_t1.p_participateinreward = '' AND (SELECT COUNT('') FROM is32bucket item_t2 WHERE ( item_t2.p_promotionuid = item_t0.p_uid ) AND (item_t2.TypePkString=? )) = '' AND EXISTS( SELECT '' FROM is32threshold item_t3 WHERE ( item_t3.p_promotionuid = item_t0.p_uid AND item_t3.p_thresholdtype = ?) AND (item_t3.TypePkString=? )) AND NOT EXISTS( SELECT '' FROM is32promoexcludeitem item_t4 WHERE ( item_t4.p_itemcode = ? and item_t4.p_bucketuid = item_t1.uniqueid ) AND (item_t4.TypePkString=? )) AND item_t0.p_status = '' AND item_t0.p_suspended = '' AND item_t0.p_startdate <= ? AND item_t0.p_enddate >= ? AND item_t0.p_basestore = ?) AND ((item_t0.TypePkString=? AND item_t1.TypePkString=? )) |
| SQL-02 | SELECT avg( item_t0.p_rating ) FROM customerreviews item_t0 WHERE ( item_t0.p_product = ?) AND (item_t0.TypePkString=? AND ( item_t0.p_blocked = '' OR item_t0.p_blocked IS NULL)AND ( item_t0.p_approvalstatus !=''))|
| SQL-03 | SELECT * FROM crmaccount WHERE PK IN (?,?,..., ?)|
| SQL-04 | select * from {Is32Promotion as p join cart as c on {p.cart} = {c.pk}} |

### 5.2 QUERY ANATOMY DECOMPOSITION PROTOCOL

**MANDATORY — applies to every FlexibleSearch or SQL query in the PR. Do NOT skip.**

**Step A — Parse and label every clause.** For each query string found, identify and list:
- `SELECT` target (PK only vs. full columns vs. aggregate function)
- `FROM` and `JOIN` (list every table and join type: INNER/LEFT/cross)
- `WHERE` conditions (list every predicate individually)
- `ORDER BY` / `GROUP BY` / `HAVING` if present
- Subqueries (list each one: correlated vs. non-correlated, scalar vs. EXISTS/NOT EXISTS)

**Step B — For each clause, ask the following questions and explicitly write your reasoning:**

| Clause | Question you MUST answer |
|--------|--------------------------|
| Every subquery inside `{{ }}` | "Is this correlated (references outer table)? If yes → flag SLOW-10 / SQL-05 / SQL-06" |
| Every `JOIN` | "What is the cardinality: 1:1, 1:N, or N:M? If 1:N or N:M without aggregation → flag SLOW-06" |
| Every `JOIN` | "Does this JOIN pattern (tables joined + join column) structurally match any JOIN in Section 5.1 patterns? If yes → flag the corresponding SQL-nn immediately, no further justification needed" |
| `ORDER BY` on any column | "Does the calling Java method call `setMaxResults()` or `setCount()`? If no → flag SLOW-05 + SLOW-07 escalated to Critical" |
| `WHERE` with 2+ columns | "Does a composite index cover this exact combination? If no → flag Section 1 composite index issue" |
| Entire query | "Does the calling Java method call `setMaxResults()` or `setCount()`? If no → flag SLOW-05 as Critical regardless of query complexity" |
| `flexibleSearchService` field | "Is it validated non-null before use (via @Required, constructor injection, or null-check)? If no → flag Section 3 NPE risk" |

**Step C — Cross-check JOIN patterns against Section 5.1.**
After extracting all JOIN clauses in Step A, compare each JOIN against the JOIN patterns defined in the SQL-nn rules in Section 5.1. Focus exclusively on the JOIN structure: which tables are joined, on which columns, and with which join type. If any JOIN in the query is structurally similar to a JOIN in a Section 5.1 pattern, you MUST flag it with the corresponding SQL-nn rule and produce a warning — regardless of the surrounding WHERE conditions, business logic, or any other context. A matching JOIN pattern alone is sufficient grounds for a mandatory warning. Do NOT attempt to assess general risk or find other justifications; the JOIN pattern match is the sole criterion.

**Step D — Determine severity using escalation.**
If a single query triggers multiple issues (e.g., unbounded result set + ORDER BY + correlated subquery), the effective severity of each issue is NEVER downgraded. Each is its own separate comment. The comment for SLOW-05 MUST be Critical even if you also flag SQL-07 separately.

**FAILURE MODES — these are automatic review failures:**
- Skipping Step A or Step B for any query in the PR
- Flagging ORDER BY without checking `setMaxResults` at the call site
- Marking Section 3 [x] in the checklist without explicitly checking every `flexibleSearchService` field for null-safety
- Writing a single comment that combines findings from two different rules (e.g., SLOW-05 + SQL-07 in one comment)
- Skipping Step C JOIN pattern check for any query in the PR

## 6. JAVA CODING PERFORMANCE

Scan every Java file for any coding patterns that negatively impact performance (CPU, memory, I/O, concurrency, thread safety). Do NOT skip this section.

## 7. REVIEW OUTPUT FORMAT

To ensure reviews are actionable, every issue MUST follow this exact format. **Do NOT use free-form paragraphs.** Every issue gets its own block:

```
### [SEVERITY: Critical/High/Medium] — Short title

**Rule**: ONE single rule index (e.g., SLOW-01 or TABLE-03)
**Location**: file:line or query line reference
**Issue**: Concrete description of what is wrong
**Evidence**: Reference to *-items.xml index definition, code line, or query pattern
**Impact**: What happens in production (e.g., "Full table scan on 10M-row Product table causing 30s response time under 100 concurrent users")
**Fix**: Specific actionable recommendation with code example if applicable
```

**Rules**:
- **ONE ISSUE = ONE COMMENT**: Each review comment MUST contain exactly ONE issue from ONE section. Do NOT group multiple issues into a single comment, even if they are in the same file or same line. Do NOT mix findings from different sections (e.g., a SLOW-xx issue and a Section 3 runtime exception MUST be two separate comments). If a code location triggers 3 different issues, produce 3 separate review comments.
- Do NOT produce vague warnings like "this query may be slow" without specifying which join/filter is the problem, which index is missing, and what the fix is.
- **Impact must be quantified** where possible: estimate table sizes, row multiplication factors, or memory consumption. "Millions of rows" is better than "many rows". "500MB heap consumed loading 2M rows of 4 columns" is better than "high memory usage".
- **Fix must include code** for Critical and High issues. A textual description alone is insufficient.

## 8. SEVERITY CLASSIFICATION GUIDE

Use the following to determine severity. Do not downgrade severity for convenience.

| Severity | Criteria | Examples |
|----------|----------|---------|
| **CRITICAL** | Causes outage, data loss, or OOM in production | Unbounded result set on 8+ table join; missing index on JOIN column of a 10M+ row table; SQL injection |
| **HIGH** | Significant performance degradation or runtime crash | NPE on common code path; client-side aggregation of large result sets; Cartesian product; missing null-check on framework return value |
| **MEDIUM** | Suboptimal performance or code quality concern | PII logging; missing composite index for multi-column filter; query not cached when it could be; missing `@Transactional(readOnly=true)` |
| **LOW** | Minor improvement opportunity | Naming conventions; missing javadoc; unused import |

**Escalation rule**: If an issue combines two categories (e.g., unbounded result set + client-side aggregation), use the HIGHER severity.

## 9. REVIEW COMPLETENESS CHECKLIST

**MANDATORY**: You MUST include this filled checklist at the END of your review. Mark each item with [x] when completed. A review missing this checklist or with unchecked mandatory items will be considered incomplete.

Before submitting the review, verify ALL sections have been evaluated:

- [ ] Section 1: **COMPLETE** index verification table produced — every JOIN ON column and every WHERE column listed (not just flagged ones)
- [ ] Section 1: Composite index check performed for multi-column WHERE filters
- [ ] Section 1: Both sides of every JOIN verified for indexes
- [ ] Section 2: All slow patterns checked (SLOW-01 through SLOW-09)
- [ ] Section 3: Java runtime exceptions scanned (NPE, unsafe cast, Optional.get, collection bounds)
- [ ] Section 4: Memory issues scanned (unbounded collections, large result sets in heap, static references)
- [ ] Section 5: Every watched table cross-checked against the query (TABLE-01 through TABLE-07) and FlexibleSearch patterns cross-checked (SQL-01 through SQL-03)
- [ ] Section 6: Java coding performance scanned
- [ ] Section 7: Every issue follows the structured output format — ONE issue per review comment, no cross-section grouping
