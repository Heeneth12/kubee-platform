package com.kubee.security;

import java.util.Collection;

/**
 * Lets a service add its own authorities to the signed-in user, e.g. POS adds POS_MANAGE for managers.
 * Every bean of this type is consulted for each authenticated request.
 */
@FunctionalInterface
public interface JwtAuthorityContributor {

    Collection<String> authorities(String userType, String roles);
}
