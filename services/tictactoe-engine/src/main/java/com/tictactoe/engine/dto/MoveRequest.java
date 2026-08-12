package com.tictactoe.engine.dto;

import com.tictactoe.engine.domain.Symbol;

public class MoveRequest {

    private Symbol symbol;
    private int position;

    public Symbol getSymbol() {
        return symbol;
    }

    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}