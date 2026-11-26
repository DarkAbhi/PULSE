# Financial account details

Bank and credit card details are stored separately in `bank_accounts` and
`credit_cards`. `financial_accounts` holds only the shared ID, owner, display name,
account kind, archive status, and timestamps. Its ID remains the reference used by
transactions and statement imports. A read-only `financial_account_details` view
provides the existing combined account API shape.

Bank accounts support an optional account number, holder name, holder email, and
savings/current type. Account numbers are text, preserving leading zeroes. Credit
cards support institution, card type, network, last four digits, holder name,
credit limit, and expected statement day (1–31). Only the last four card digits
can be entered. Lists show masked identifiers; full bank numbers appear in edit
forms. Account forms and requests contain neither currency nor country code.
Limits are entered in the profile currency. Statement uploads and commits check
the current profile currency against the statement/batch currency.

Migration `00042_financial_account_details` intentionally discards development
accounts, as authorized because accounts have not been deployed to production.
It clears account links from existing transactions, retains their amounts and
history, and converts imported transactions to manual transactions by clearing
their import references. Deleting the old accounts also deletes their statement
import batches, preview rows, structures, and deduplication records. This is not a
data-preserving account migration. Rollback also discards accounts and their
import metadata; it does not restore deleted development data.

Apply the migration through the normal backend migration command before running
the updated backend, web frontend, and Android app. The account contract changes
require updating clients together; older Android builds still expect account
currency/country fields. No production or development database is migrated by
the code checks in this change.

Verification covers account validation, separate-table persistence, migration
reset/rollback, retained transaction history, ownership, archive/restore, account
identity locks, currency changes between preview and commit, web create/edit
round trips, and Android DTO/request round trips and editor validation.
