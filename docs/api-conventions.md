# API conventions

Collection paths use plural resource names. A singleton belonging to the current
user can be singular: `/api/cart`, `/reaction`, `/preview-image`.

- GET reads data.
- POST creates collection members or invokes business commands.
- PATCH partially updates a resource.
- PUT replaces a singleton, including the current user's reaction.
- DELETE removes a resource or the current user's reaction.
- Creation returns 201. A response representation returns 200. Successful changes
  without a representation return 204.

## Updated contracts

| Method | Path | Body / meaning |
| --- | --- | --- |
| GET | `/api/admin/users/{userId}/cart` | Administrative cart lookup |
| POST | `/api/cart/items` | `{ "productId": "UUID" }`; add or increment; 204 |
| PATCH | `/api/cart/items/{itemId}` | `{ "quantity": 2 }`; 204 |
| POST | `/api/reviews/{reviewId}/replies` | `{ "text": "...", "replyToId": null }`; 201 |
| PUT | `/api/reviews/{reviewId}/reaction` | `{ "type": "LIKE" }` or `DISLIKE`; 204 |
| DELETE | `/api/reviews/{reviewId}/reaction` | 204 |
| PUT | `/api/review-replies/{replyId}/reaction` | Same reaction body; 204 |
| DELETE | `/api/review-replies/{replyId}/reaction` | 204 |

Product and review moderation requests use `/api/moderation/product-requests`
and `/api/moderation/review-requests`; each supports GET collection, GET by ID,
POST `/{requestId}/approve`, POST `/{requestId}/reject`.

Reply postmoderation uses GET `/api/moderation/review-replies?status=PUBLISHED`
and PATCH `/api/moderation/review-replies/{reviewReplyId}` with
`{ "status": "HIDDEN" }` or `PUBLISHED`.

Product `publish`, `withdraw`, `archive`, and order `cancel`, `deliver` commands
use POST. Existing moderation submission and withdrawal commands remain POST.
Catalog, seller selection, current orders and history remain separate views.

Removed paths have no compatibility aliases. Update external API clients together
with the application. Security is enforced independently from path naming.
