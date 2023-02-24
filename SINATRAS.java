package es.informance.uv.pseudopastillas;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import core_r6.Modelo.Lanza_Proceso;
import es.informance.tactica.plugins.jira.ProxyJira;
import es.uv.util.SeuBD;

public class SINATRAS {

    private static final String IDFORM = "SINATRAS";

    private static final String NOMBRE_PADRE = "[%ACR%] - %TIT%";
    private static final String NOMBRE_FES = "[%ACR%] - Reedición FES";
    private static final String NOMBRE_FIV = "[%ACR%] - Reedición FIV";
    private static final String NOMBRE_FES_NUEVO = "[%ACR%] - Creación FES";
    private static final String NOMBRE_FIV_NUEVO = "[%ACR%] - Creación FIV";
    private static final String NOMBRE_FES_PROD = "[%ACR%] - Paso a producción FES";
    private static final String NOMBRE_FIV_PROD = "[%ACR%] - Paso a producción FIV";

    private static final String NOMCAMPO_PADRE = "JIRA_KEY_ID_MASTER";
    private static final String NOMCAMPO_FES = "JIRA_KEY_ID_FES_TEST";
    private static final String NOMCAMPO_FIV = "JIRA_KEY_ID_FIV_TEST";
    private static final String NOMCAMPO_FES_PROD = "JIRA_KEY_ID_FES_PROD";
    private static final String NOMCAMPO_FIV_PROD = "JIRA_KEY_ID_FIV_PROD";

    private static final String NOMCAMPO_NOMBRE = "nombre";
    private static final String NOMCAMPO_TITULO = "titulo";
    private static final String NOMCAMPO_TAREA_EDICION_ANTERIOR = "css";
    private static final String NOMCAMPO_FECH_INI = "fechaIni";
    private static final String NOMCAMPO_FECH_FIN = "fechaFin";
    private static final String NOMCAMPO_ES_REEDICION = "es_reedicion";

    private static final String JIRA_PROYECTO = "JIRA_PROJECT";
    private static final String JIRA_ADJUNTAR_DOCUMENTACION = "JIRA_ADJDOC";
    private static final String JIRA_FASE_DOCUMENTACION = "JIRA_FASE_DOCS";
    private static final String JIRA_TIPO_TAREA = "JIRA_ISSUETYPE";
    private static final String JIRA_DESCRIPCION = "JIRA_DESCRIPTION";
    private static final String JIRA_OTROS_CAMPOS = "JIRA_CMFIELD_EXTRAS";

    private static final int FILA_LISTA_99 = 99;

    private final Logger logger = Logger.getLogger(this.getClass());
    
    

    public void crearTareasJira(Connection con, Long idenvio, int fase, Lanza_Proceso proceso, Hashtable<String, Object> parametros)
    		throws Exception {
        logger.info("[" + IDFORM + "] Inicio crearTareasJira");

        try {
            SeuBD seubd = new SeuBD(con);

            String nombreFES = "";
        	String nombreFIV = "";
            
            //Recupera parametros de Jira
            String proyecto = seubd.getParametro(IDFORM, JIRA_PROYECTO).trim();
            String tipoTarea = seubd.getParametro(IDFORM, JIRA_TIPO_TAREA).trim();
            String descripcion = seubd.getParametro(IDFORM, JIRA_DESCRIPCION).trim();
            String adjuntarDoc = seubd.getParametro(IDFORM, JIRA_ADJUNTAR_DOCUMENTACION).trim();
            String otrosCampos = seubd.getParametro(IDFORM, JIRA_OTROS_CAMPOS).trim();

            //Recupera valores de la solicitud
            String acronimo = seubd.getValor(String.valueOf(idenvio), NOMCAMPO_NOMBRE).trim();
            String titulo = seubd.getValor(String.valueOf(idenvio), NOMCAMPO_TITULO).trim();
            String tareaEdicionAnterior = seubd.getValor(String.valueOf(idenvio), NOMCAMPO_TAREA_EDICION_ANTERIOR).trim();
            String fechaIni = seubd.getValor(String.valueOf(idenvio), NOMCAMPO_FECH_INI).trim();
            String fechaFin = seubd.getValor(String.valueOf(idenvio), NOMCAMPO_FECH_FIN).trim();            
            String reedicion = seubd.getValor(String.valueOf(idenvio), NOMCAMPO_ES_REEDICION).trim();

            descripcion = reemplazaMarcadores(descripcion, acronimo, titulo, fechaIni, fechaFin,idenvio.toString());
            otrosCampos = reemplazaMarcadores(otrosCampos, acronimo, titulo, fechaIni, fechaFin, idenvio.toString());

            //Establece los nombres de las tareas
            String nombrePadre = reemplazaMarcadores(NOMBRE_PADRE, acronimo, titulo, fechaIni, fechaFin, idenvio.toString());
            if (reedicion.equals("S")) {
            	nombreFES = reemplazaMarcadores(NOMBRE_FES, acronimo, titulo, fechaIni, fechaFin, idenvio.toString());
            	nombreFIV = reemplazaMarcadores(NOMBRE_FIV, acronimo, titulo, fechaIni, fechaFin, idenvio.toString());
            }
            else{
            	nombreFES = reemplazaMarcadores(NOMBRE_FES_NUEVO, acronimo, titulo, fechaIni, fechaFin, idenvio.toString());
            	nombreFIV = reemplazaMarcadores(NOMBRE_FIV_NUEVO, acronimo, titulo, fechaIni, fechaFin, idenvio.toString());
            }
            String nombreFESProd = reemplazaMarcadores(NOMBRE_FES_PROD, acronimo, fechaIni, fechaFin, titulo, idenvio.toString());
            String nombreFIVProd = reemplazaMarcadores(NOMBRE_FIV_PROD, acronimo, fechaIni, fechaFin, titulo, idenvio.toString());

            //Crea tareas Jira
            String keyTareaPadre = crearTareaJira(proyecto, nombrePadre, descripcion, tipoTarea, tareaEdicionAnterior, otrosCampos);
            seubd.setValor(String.valueOf(idenvio), NOMCAMPO_PADRE, keyTareaPadre, FILA_LISTA_99);
            logger.info("La tarea padre es: " + keyTareaPadre);

            String keyTareaFES = crearTareaJira(proyecto, nombreFES, descripcion, tipoTarea, keyTareaPadre, otrosCampos);
            seubd.setValor(String.valueOf(idenvio), NOMCAMPO_FES, keyTareaFES, FILA_LISTA_99);

            String keyTareaFIV = crearTareaJira(proyecto, nombreFIV, descripcion, tipoTarea, keyTareaPadre, otrosCampos);
            seubd.setValor(String.valueOf(idenvio), NOMCAMPO_FIV, keyTareaFIV, FILA_LISTA_99);

            String keyTareaFESProd = crearTareaJira(proyecto, nombreFESProd, descripcion, tipoTarea, keyTareaPadre, otrosCampos);
            seubd.setValor(String.valueOf(idenvio), NOMCAMPO_FES_PROD, keyTareaFESProd, FILA_LISTA_99);

            String keyTareaFIVProd = crearTareaJira(proyecto, nombreFIVProd, descripcion, tipoTarea, keyTareaPadre, otrosCampos);
            seubd.setValor(String.valueOf(idenvio), NOMCAMPO_FIV_PROD, keyTareaFIVProd, FILA_LISTA_99);

            //Adjuntar documentos
            if (adjuntarDoc != null && adjuntarDoc.equals("S")) {
                List<String> rutasAdjuntos = devolverRutasFicherosAdjuntos(seubd, con, idenvio);
                adjuntarFicherosJira(keyTareaPadre, rutasAdjuntos);
            }
        } catch (Exception e) {
            System.out.println("[" + IDFORM + "] Error: " + e.getMessage());
            logger.error("[" + IDFORM + "] Error: " + e.getMessage());
            logger.error(null, e);
            throw e;
        }

        logger.info("[" + IDFORM + "] Fin crearTareasJira");
    }

	private String crearTareaJira(String proyecto, String nombre, String descripcion, String nombreTipoTarea, String padre, String otrosCampos) throws Exception {
        logger.info("[" + IDFORM + "] Creando tarea: " + nombre);

        String keyTarea = "";

        ProxyJira jira = new ProxyJira();

        JSONObject data = new JSONObject();
        JSONObject tarea = new JSONObject();

        JSONObject proyectoTarea = new JSONObject("{\"key\":\"" + proyecto + "\"}");
        JSONObject tipoTarea = new JSONObject("{\"id\": \"" + nombreTipoTarea + "\"}");
        
        logger.info("[" + IDFORM + "] Valores tarea: " + proyecto + ", " + nombre + ", " + descripcion + ", " + nombreTipoTarea + ", " + padre + ", " + otrosCampos);
        logger.info("[" + IDFORM + "] Creando tarea: " + tipoTarea + ", " + proyectoTarea);

        tarea.put("project", proyectoTarea);
        tarea.put("summary", nombre);
        tarea.put("issuetype", tipoTarea);
        tarea.put("description", descripcion);
       
        if(otrosCampos.length() > 0){
            JSONObject jsonFieldOtros = new JSONObject(otrosCampos);
            for (Iterator iterator = jsonFieldOtros.keys(); iterator.hasNext();) {
                String key = (String) iterator.next();
                Object objeto = jsonFieldOtros.get(key);

                tarea.put(key, objeto);
            }
        }

        data.put("fields", tarea);
        
        //Se vinculan a la tarea general el resto de tareas
        
        if (padre != null && padre.length() > 0) {
            JSONObject padreTarea = new JSONObject(
                    "{ \"issuelinks\":[ { \"add\":{ \"type\":{ \"name\": \"Relacionar\", \"inward\": \"relacionat amb/relacionado con\", \"outward\": \"relacionat amb/relacionado con\" }, \"outwardIssue\":{ \"key\":\""
                            + padre + "\" } } } ] }");
            
            data.put("update", padreTarea);
            
            logger.info("[" + IDFORM + "] Actualizacion padre tarea: " + padreTarea);
        }

        keyTarea = jira.crearTarea(data);

        logger.info("[" + IDFORM + "] Tarea creada: " + keyTarea);
        return keyTarea;
    }

    private void adjuntarFicherosJira(String keyTarea, List<String> rutas) throws Exception {
        logger.info("[" + IDFORM + "] Adjuntando ficheros a la tarea: " + keyTarea);

        ProxyJira jira = new ProxyJira();
        boolean exito = true;

        for (String ruta : rutas) {
            exito = jira.adjuntarArchivoTarea(keyTarea, ruta);

            if (!exito) {
                logger.info("[" + IDFORM + "] Error adjuntando fichero: " + ruta);
            }
        }

        logger.info("[" + IDFORM + "] Fin adjuntar ficheros a: " + keyTarea);
    }


    private List<String> devolverRutasFicherosAdjuntos(SeuBD seubd, Connection con, Long idenvio) throws Exception {
        String rutaPDF = "";
        List<String> resultados = new ArrayList<String>();

        String faseDocs = seubd.getParametro(IDFORM, JIRA_FASE_DOCUMENTACION);
        
        Vector<Vector<String>> docsSolicitud = seubd.getSelectCompleja("select valcampo from campos_valor where numdoc='"+idenvio+"' and NOMCAMPO in ('docConvocatoria', 'excelUsuarios', 'mapaDoc', 'Anexo1', 'Anexo2', 'Anexo3') and fila_lista = 0");

        for(Vector<String> docActual : docsSolicitud){
        	resultados.add("/nas/UVSede/configuracion/env/"+docActual.get(0));
        }
        
        return resultados;
    }

    //Reemplaza las variabes %ACR%, %TIT% y %EXP% por los valores indicados en la solicitud
    private String reemplazaMarcadores(String texto, String acronimo, String titulo, String fechaIni, String fechaFin, String expediente) {

        String resultado = texto.replaceAll("%ACR%", acronimo).replaceAll("%TIT%", titulo).replaceAll("%EXP%", expediente).replaceAll("%fecha_ini%", fechaIni).replaceAll("%fecha_fin%", fechaFin);

        return resultado;
    }

}
