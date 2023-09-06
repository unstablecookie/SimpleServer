# SimpleServer
server waits for incoming multipart post requests from simple client

сервер основан на com.sun.net.httpserver , использует 4 отдельных процесса для обрабоки входящих post multipart запросов на портах 8001,8002,8003,8004.
обработчик запросов ориентирован только на simple client ввиду специфичной обработки заголовка http запроса.
