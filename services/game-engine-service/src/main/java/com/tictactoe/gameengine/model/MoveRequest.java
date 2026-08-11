package com.tictactoe.gameengine.model;

public class MoveRequest {

    private Player player;
    private int position;

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}