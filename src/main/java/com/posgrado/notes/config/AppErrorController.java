package com.posgrado.notes.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AppErrorController implements ErrorController {

    
    @RequestMapping(value = "/error", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String handleError(HttpServletRequest request) {
        
        Object statusAttr = request.getAttribute("jakarta.servlet.error.status_code");
        String paramStatus = request.getParameter("status");
        
        int statusCode = 500; 
        if (paramStatus != null) {
            statusCode = Integer.parseInt(paramStatus);
        } else if (statusAttr != null) {
            statusCode = Integer.parseInt(statusAttr.toString());
        }

    
        String title = "Error " + statusCode;
        String description = "Ocurrió un problema inesperado procesando tu solicitud.";

        if (statusCode == 404) {
            description = "La página o recurso que intentas buscar no existe.";
        } else if (statusCode == 403) {
            description = "Acceso Denegado. No tienes los permisos necesarios (Rol incorrecto).";
        } else if (statusCode == 401) {
            title = "Sesión requerida";
            description = "Debes iniciar sesión para acceder a este recurso.";
        } else if (statusCode == 400) {
            description = "Petición incorrecta o datos inválidos.";
        }

        // 3. Evaluar la sesión actual usando SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser");

        String targetUrl = isAuthenticated ? "/index.html" : "/login.html";
        String buttonText = isAuthenticated ? "Volver al Inicio" : "Ir a Iniciar Sesión";

        // 5. Retornar la estructura HTML (Ventana de error)
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f3f4f6; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
                    .error-card { background: white; padding: 40px; border-radius: 12px; box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1); max-width: 450px; text-align: center; }
                    h1 { color: #dc2626; margin-top: 0; font-size: 2.5rem; }
                    p { color: #4b5563; font-size: 1.1rem; line-height: 1.5; margin-bottom: 30px; }
                    .btn { display: inline-block; background-color: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 8px; font-weight: bold; transition: background-color 0.2s; }
                    .btn:hover { background-color: #1d4ed8; }
                </style>
            </head>
            <body>
                <div class="error-card">
                    <h1>%s</h1>
                    <p>%s</p>
                    <a href="%s" class="btn">%s</a>
                </div>
            </body>
            </html>
            """.formatted(title, title, description, targetUrl, buttonText);
    }
}