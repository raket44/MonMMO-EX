package pirated.server.login.session

import pirated.network.SessionAttribute

/** The account id a login session authenticated as, sent on to the game server when it joins. */
val AUTHED_USER_ID = SessionAttribute.of<Int>("authedUserId")
