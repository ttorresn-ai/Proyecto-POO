package org.UNAL;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.UNAL.modelo.Usuario;
import org.UNAL.views.BienvenidaView;
import org.UNAL.views.LoginView;
import org.UNAL.views.RegisterView;
import org.UNAL.views.DashboardView;
import org.UNAL.views.CuentaView;
import org.UNAL.views.TransaccionView;
import org.UNAL.views.PresupuestoView;
import org.UNAL.views.ReporteView;
import org.UNAL.views.SplashView;

public class FinanzasApp extends Application {

    private Stage primaryStage;
    private StackPane rootLayout;

    private BienvenidaView bienvenidaView;
    private LoginView loginView;
    private RegisterView registerView;

    // Propiedades globales para evitar errores de inicialización local en Lambdas
    private Scene escenaBase;
    private Scene escenaReiniciada;
    private VBox formularioActivo = null;
    private Usuario usuarioActual;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        SplashView splashView = new SplashView();

        // CONTROL DEL SPLASH FLUIDO: Pasa a la bienvenida inmediatamente al terminar el tiempo
        Scene escenaSplash = splashView.getScene(() -> {
            Platform.runLater(() -> {
                this.bienvenidaView = new BienvenidaView();
                this.loginView = new LoginView();
                this.registerView = new RegisterView();

                // Pasamos un callback vacío () -> {} en la inicialización inicial
                // para que el Splash no dependa del hardware de video al arrancar.
                this.escenaBase = bienvenidaView.getScene(
                        this::mostrarLogin,
                        this::mostrarRegistro,
                        () -> { /* El video iniciará solo internamente en segundo plano */ }
                );

                // Forzamos el renderizado inmediato en el hilo de la UI
                this.rootLayout = (StackPane) this.escenaBase.getRoot();
                this.primaryStage.setScene(this.escenaBase);
                this.primaryStage.centerOnScreen();
                System.out.println("// TRACE: ARRANQUE EXITOSO -> BIENVENIDA DESPLEGADA");
            });
        });

        primaryStage.setTitle("NEXUS FINANCE // CORE_SYSTEM");
        primaryStage.setScene(escenaSplash);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void mostrarLogin() {
        limpiarContenedoresFlotantes();
        formularioActivo = loginView.getContainer(
                this.bienvenidaView,
                this::manejarLoginExitoso,
                this::mostrarRegistro,
                this::volverAlMenuBienvenida
        );
        if (rootLayout != null) {
            rootLayout.getChildren().add(formularioActivo);
            if (!rootLayout.getChildren().isEmpty()) {
                rootLayout.getChildren().get(0).setMouseTransparent(true);
            }
        }
    }

    private void mostrarRegistro() {
        limpiarContenedoresFlotantes();
        formularioActivo = registerView.getContainer(
                this::manejarRegistroExitoso,
                this::volverAlMenuBienvenida
        );
        if (rootLayout != null) {
            rootLayout.getChildren().add(formularioActivo);
            if (!rootLayout.getChildren().isEmpty()) {
                rootLayout.getChildren().get(0).setMouseTransparent(true);
            }
        }
    }

    private void limpiarContenedoresFlotantes() {
        if (rootLayout != null && formularioActivo != null) {
            rootLayout.getChildren().remove(formularioActivo);
            formularioActivo = null;
        }
        if (rootLayout != null && rootLayout.getChildren().size() >= 1) {
            rootLayout.getChildren().get(0).setMouseTransparent(false);
            rootLayout.getChildren().get(0).setDisable(false);
        }
    }

    private void manejarLoginExitoso(Usuario usuario) {
        System.out.println("// TRACE: SESIÓN INICIADA PARA -> " + usuario.getNombre());
        this.usuarioActual = usuario;
        Platform.runLater(() -> {
            try {
                if (bienvenidaView != null) {
                    bienvenidaView.detenerMultimedia();
                }
                limpiarContenedoresFlotantes();

                // Espera corta para dar tiempo a liberar recursos nativos del MediaPlayer
                javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(150));
                delay.setOnFinished(ev -> mostrarDashboard());
                delay.play();

            } catch (Exception e) {
                System.err.println("CRITICAL ERROR: Fallo al cargar DashboardView");
                e.printStackTrace();
            }
        });
    }

    private void mostrarDashboard() {
        try {
            DashboardView dashboard = new DashboardView();
            Scene nuevaEscena = dashboard.getScene(
                    usuarioActual.getNombre(),
                    0.0,
                    this::mostrarCuentas,
                    this::mostrarIngreso,
                    this::mostrarGasto,
                    this::mostrarPresupuestos,
                    this::mostrarReportes,
                    this::ejecutarCierreSesionLimpio
            );

            this.primaryStage.setScene(nuevaEscena);
            this.primaryStage.centerOnScreen();
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Fallo al cargar DashboardView (dentro de delay)");
            e.printStackTrace();
        }
    }

    private void mostrarCuentas() {
        try {
            CuentaView cuentaView = new CuentaView(usuarioActual);
            Scene escenaCuentas = cuentaView.getScene(this::mostrarDashboard);
            this.primaryStage.setScene(escenaCuentas);
            this.primaryStage.centerOnScreen();
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Fallo al cargar CuentaView");
            e.printStackTrace();
        }
    }

    private void mostrarIngreso() {
        try {
            TransaccionView transaccionView = new TransaccionView(usuarioActual, true);
            Scene escenaIngreso = transaccionView.getScene(this::mostrarDashboard);
            this.primaryStage.setScene(escenaIngreso);
            this.primaryStage.centerOnScreen();
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Fallo al cargar TransaccionView (Ingreso)");
            e.printStackTrace();
        }
    }

    private void mostrarGasto() {
        try {
            TransaccionView transaccionView = new TransaccionView(usuarioActual, false);
            Scene escenaGasto = transaccionView.getScene(this::mostrarDashboard);
            this.primaryStage.setScene(escenaGasto);
            this.primaryStage.centerOnScreen();
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Fallo al cargar TransaccionView (Gasto)");
            e.printStackTrace();
        }
    }

    private void mostrarPresupuestos() {
        try {
            PresupuestoView presupuestoView = new PresupuestoView(usuarioActual);
            Scene escenaPresupuestos = presupuestoView.getScene(this::mostrarDashboard);
            this.primaryStage.setScene(escenaPresupuestos);
            this.primaryStage.centerOnScreen();
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Fallo al cargar PresupuestoView");
            e.printStackTrace();
        }
    }

    private void mostrarReportes() {
        try {
            ReporteView reporteView = new ReporteView(usuarioActual);
            Scene escenaReportes = reporteView.getScene(this::mostrarDashboard);
            this.primaryStage.setScene(escenaReportes);
            this.primaryStage.centerOnScreen();
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Fallo al cargar ReporteView");
            e.printStackTrace();
        }
    }

    // CIERRE DE SESIÓN ASÍNCRONO SEGURO CON CONTROL DE BUFFER DE VIDEO
    private void ejecutarCierreSesionLimpio() {
        System.out.println("// TRACE: SOLICITANDO REINICIO SEGURO DE APP (LOGOUT)");
        Platform.runLater(() -> {
            if (bienvenidaView != null) {
                bienvenidaView.detenerMultimedia();
            }
            this.formularioActivo = null;
            this.rootLayout = null;

            // Re-instanciamos vistas completamente limpias para liberar memoria corrupta
            this.bienvenidaView = new BienvenidaView();
            this.loginView = new LoginView();
            this.registerView = new RegisterView();

            // Al hacer LOGOUT, aquí sí esperamos que el video esté listo antes de pintar la escena,
            // evitando que el formulario del login se quede congelado sobre una pantalla negra.
            this.escenaReiniciada = bienvenidaView.getScene(
                    this::mostrarLogin,
                    this::mostrarRegistro,
                    () -> {
                        this.rootLayout = (StackPane) this.escenaReiniciada.getRoot();
                        this.primaryStage.setScene(this.escenaReiniciada);
                        this.primaryStage.centerOnScreen();
                        System.out.println("// TRACE: BIENVENIDA RESTAURADA TRAS LOGOUT");
                    }
            );
        });
    }

    private void manejarRegistroExitoso() {
        mostrarLogin();
    }

    private void volverAlMenuBienvenida() {
        limpiarContenedoresFlotantes();
    }

    @Override
    public void stop() {
        if (bienvenidaView != null) bienvenidaView.detenerMultimedia();
    }

    public static void main(String[] args) {
        launch(args);
    }
}