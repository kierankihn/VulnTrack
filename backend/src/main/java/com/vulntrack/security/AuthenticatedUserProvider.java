package com.vulntrack.security;

import com.vulntrack.entity.User;

public interface AuthenticatedUserProvider {

    User getCurrentUserOrThrow();
}
