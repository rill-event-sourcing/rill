# Login flow

The entry point in the Studyflow system is the so-called __login__ application.

## Stored Credentials

The stored credentials are the following

* user-id (uuid)
* encrypted\_password (encrypted with _bcrypt_)
* role (plain text role)

## Redis session-database content

When a user is logged in, the Redis session-database contains the following pairs of keys-values.

| key               | value        |
|-------------------|--------------|
| `user-id`         | `user-role`  |
| `session-id`      | `user-id`    |

**Note**: Throughout this document, when we write `expression`, we refer to the expression value.

## Logging in

1. Upon insertion of credentials, the provided password is encrypted with _bcrypt_ and checked against the stored encrypted password.

1. On positive match, a new series of values is created in the Redis database, following the scheme of the previous section.

1. A cookie named "studyflow_session" is created, containing `session-id`

1. The application looks for the cookie "studyflow_redir_to": if present the user is redirected to the URL contained in the cookie, otherwise it is simply redirected to the default value according to its role.

## Logout

Logout is done with a DELETE request from the original application to the root path of the login app.
