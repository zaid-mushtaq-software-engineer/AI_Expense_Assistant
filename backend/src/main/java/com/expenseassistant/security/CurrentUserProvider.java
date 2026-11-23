package com.expenseassistant.security;

public interface CurrentUserProvider {

    Long requireUserId();
}