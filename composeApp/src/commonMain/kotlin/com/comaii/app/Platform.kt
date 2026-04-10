package com.comaii.app

/** Retorna o host base da aplicação (ex: "localhost:8080" em dev, "comaii.com" em prod) */
expect fun getStoreBaseUrl(): String
