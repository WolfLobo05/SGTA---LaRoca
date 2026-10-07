package com.laroca.controlador;

import com.laroca.conexion.Conexion;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;

@WebServlet("/prueba-conexion")
public class PruebaConexionServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        Connection conexion = Conexion.conectar();

        if (conexion != null) {
            response.getWriter().println("<h1>¡Conexión exitosa a MySQL!</h1>");

            try {
                conexion.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
    response.getWriter().println("<h1>No se pudo conectar a MySQL.</h1>");
    response.getWriter().println("<p>Revisa la consola de Tomcat para ver el error de MySQL.</p>");
}
    }
}