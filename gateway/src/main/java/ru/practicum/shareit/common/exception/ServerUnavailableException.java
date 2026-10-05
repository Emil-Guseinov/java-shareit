package ru.practicum.shareit.common.exception;

public class ServerUnavailableException extends RuntimeException {
    public ServerUnavailableException(Throwable cause) {
        super("Сервер ShareIt недоступен", cause);
    }
}
