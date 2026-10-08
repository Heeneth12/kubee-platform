package com.kubee.pos.shift.domain;

/** IN: cash added to the drawer (float, change). OUT: cash taken out (supplier paid, bank deposit, expense). */
public enum CashMovementType {
    IN, OUT
}
