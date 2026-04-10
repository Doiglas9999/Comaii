// Habilita SPA routing: redireciona todas as rotas para index.html
// Necessário para que /slug funcione sem "Cannot GET /slug"
config.devServer = config.devServer || {};
config.devServer.historyApiFallback = true;
