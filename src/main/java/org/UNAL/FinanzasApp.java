package org.UNAL;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.UNAL.config.ConexionDB;
import org.UNAL.modelo.Usuario;
import org.UNAL.views.BienvenidaView;
import org.UNAL.views.CuentaView;
import org.UNAL.views.CyberAlert;
import org.UNAL.views.DashboardView;
import org.UNAL.views.LoginView;
import org.UNAL.views.PresupuestoView;
import org.UNAL.views.RegisterView;
import org.UNAL.views.ReporteView;
import org.UNAL.views.SplashView;
import org.UNAL.views.TransaccionView;

/**
 * Punto de entrada y orquestador de navegación.
 *
 * <p>Todas las pantallas se montan sobre un único {@link Stage}. La navegación se
 * resuelve sustituyendo la {@link Scene}, y cada vista recibe un callback
 * {@code onVolver} para regresar: así ninguna vista depende del Stage y todas
 * siguen siendo independientes de probar.
 *
 * <p>Flujo: Splash -> Bienvenida -> (Login | Registro) -> Dashboard -> Módulos
 */
public class FinanzasApp extends Application {

    /** Respiro para que los hilos nativos del MediaPlayer liberen recursos. */
    private static final long RETRASO_LIBERACION_MEDIA_MS = 150;

    private Stage primaryStage;

    // Vistas recreadas en cada entrada a la pantalla de bienvenida
    private BienvenidaView bienvenidaView;
    private LoginView loginView;
    private RegisterView registerView;

    // Área autenticada
    private DashboardView dashboardView;
    private Usuario usuarioActual;

    // Estado del formulario flotante sobre la bienvenida
    private StackPane rootLayout;
    private VBox formularioActivo;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("NEXUS FINANCE // CORE_SYSTEM");
        primaryStage.setResizable(false);

        Scene escenaSplash = new SplashView().getScene(this::desplegarBienvenida);
        primaryStage.setScene(escenaSplash);
        primaryStage.show();
    }

    // -----------------------------------------------------------------------
    //  Pantalla de bienvenida
    // -----------------------------------------------------------------------

    private void desplegarBienvenida() {
        Platform.runLater(() -> {
            bienvenidaView = new BienvenidaView();
            loginView = new LoginView();
            registerView = new RegisterView();
            usuarioActual = null;
            dashboardView = null;

            // El callback se invoca cuando el video está listo; evita un frame negro
            // mientras el decodificador arranca. Montamos la escena de inmediato para
            // que la interfaz sea interactiva desde el primer instante.
            Scene escena = bienvenidaView.getScene(
                    this::mostrarLogin,
                    this::mostrarRegistro,
                    () -> { /* el video ya se está reproduciendo detrás */ }
            );

            rootLayout = (StackPane) escena.getRoot();
            formularioActivo = null;

            primaryStage.setResizable(false);
            primaryStage.setScene(escena);
            primaryStage.centerOnScreen();
            System.out.println("// TRACE: ARRANQUE EXITOSO -> BIENVENIDA DESPLEGADA");
        });
    }

    private void mostrarLogin() {
        if (bienvenidaView == null || loginView == null) {
            return;
        }
        limpiarFormularioFlotante();
        formularioActivo = loginView.getContainer(
                bienvenidaView,
                this::manejarLoginExitoso,
                this::mostrarRegistro,
                this::volverAlMenuBienvenida
        );
        superponerFormulario();
    }

    private void mostrarRegistro() {
        if (registerView == null) {
            return;
        }
        limpiarFormularioFlotante();
        formularioActivo = registerView.getContainer(
                this::manejarRegistroExitoso,
                this::volverAlMenuBienvenida
        );
        superponerFormulario();
    }

    /**
     * Añade el formulario como capa flotante sobre el menú lateral y bloquea los
     * clics del menú para que la pantalla no quede accesible detrás del formulario.
     */
    private void superponerFormulario() {
        if (rootLayout == null || formularioActivo == null) {
            return;
        }
        rootLayout.getChildren().add(formularioActivo);
        setMenuLateralClickable(false);
    }

    /**
     * Elimina el formulario flotante y devuelve el control al menú lateral.
     */
    private void limpiarFormularioFlotante() {
        if (rootLayout != null && formularioActivo != null) {
            rootLayout.getChildren().remove(formularioActivo);
            formularioActivo = null;
        }
        setMenuLateralClickable(true);
    }

    /**
     * BienvenidaView monta sus capas como [video, interfaz]. La segunda es el
     * AnchorPane que contiene el menú lateral y los botones.
     */
    private void setMenuLateralClickable(boolean clickable) {
        if (rootLayout == null) {
            return;
        }
        var hijos = rootLayout.getChildren();
        if (hijos.size() >= 2) {
            hijos.get(1).setMouseTransparent(!clickable);
        }
    }

    private void volverAlMenuBienvenida() {
        limpiarFormularioFlotante();
    }

    // -----------------------------------------------------------------------
    //  Autenticación
    // -----------------------------------------------------------------------

    private void manejarLoginExitoso(Usuario usuario) {
        usuarioActual = usuario;
        System.out.println("// TRACE: SESIÓN INICIADA PARA -> " + usuario.getNombre());

        Platform.runLater(() -> {
            if (bienvenidaView != null) {
                bienvenidaView.detenerMultimedia();
            }
            if (rootLayout != null) {
                rootLayout.getChildren().clear();
                rootLayout = null;
            }
            formularioActivo = null;

            PauseTransition delay = new PauseTransition(Duration.millis(RETRASO_LIBERACION_MEDIA_MS));
            delay.setOnFinished(ev -> abrirDashboard());
            delay.play();
        });
    }

    private void manejarRegistroExitoso() {
        mostrarLogin();
    }

    private void abrirDashboard() {
        Platform.runLater(() -> {
            if (usuarioActual == null) {
                return;
            }
            try {
                // Reutilizamos la instancia para no acumular MediaPlayers huérfanos.
                if (dashboardView == null) {
                    dashboardView = new DashboardView();
                }

                Scene escena = dashboardView.getScene(
                        usuarioActual,
                        this::abrirCuentas,
                        () -> abrirTransaccion(true),
                        () -> abrirTransaccion(false),
                        this::abrirPresupuestos,
                        this::abrirReportes,
                        this::ejecutarCierreSesionLimpio
                );

                primaryStage.setResizable(true);
                primaryStage.setScene(escena);
                primaryStage.centerOnScreen();
            } catch (Exception e) {
                e.printStackTrace();
                CyberAlert.mostrarError("No se pudo cargar el panel principal: " + e.getMessage());
            }
        });
    }

    // -----------------------------------------------------------------------
    //  Módulos autenticados
    // -----------------------------------------------------------------------

    private void abrirCuentas() {
        prepararModulo();
        new CuentaView(usuarioActual).mostrarEn(this.primaryStage, this::volverAlDashboard);
    }

    private void abrirTransaccion(boolean esIngreso) {
        prepararModulo();
        TransaccionView vista = new TransaccionView(usuarioActual, esIngreso);
        vista.mostrarEn(this.primaryStage, this::volverAlDashboard);
    }

    private void abrirPresupuestos() {
        prepararModulo();
        new PresupuestoView(usuarioActual).mostrarEn(this.primaryStage, this::volverAlDashboard);
    }

    private void abrirReportes() {
        prepararModulo();
        new ReporteView(usuarioActual).mostrarEn(this.primaryStage, this::volverAlDashboard);
    }

    /**
     * Libera el video del dashboard antes de montar un módulo, para no dejar
     * decodificadores nativos ejecutándose detrás de la nueva pantalla.
     */
    private void prepararModulo() {
        if (dashboardView != null) {
            dashboardView.detenerMultimedia();
        }
    }

    private void volverAlDashboard() {
        Platform.runLater(this::abrirDashboard);
    }

    // -----------------------------------------------------------------------
    //  Cierre de sesión
    // -----------------------------------------------------------------------

    private void ejecutarCierreSesionLimpio() {
        System.out.println("// TRACE: SOLICITANDO REINICIO SEGURO DE APP (LOGOUT)");
        Platform.runLater(() -> {
            usuarioActual = null;

            if (dashboardView != null) {
                dashboardView.detenerMultimedia();
                dashboardView = null;
            }
            if (rootLayout != null) {
                rootLayout.getChildren().clear();
                rootLayout = null;
            }
            formularioActivo = null;

            desplegarBienvenida();
        });
    }

    @Override
    public void stop() {
        // Liberamos los recursos nativos antes de cerrar el pool: los MediaPlayer
        // mantienen hilos activos hasta que se detienen explícitamente.
        if (bienvenidaView != null) {
            bienvenidaView.detenerMultimedia();
        }
        if (dashboardView != null) {
            dashboardView.detenerMultimedia();
        }
        ConexionDB.cerrarPool();
    }

    public static void main(String[] args) {
        launch(args);
    }
}