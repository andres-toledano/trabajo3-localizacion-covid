package com.practica.ems.covid;


import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.practica.excecption.EmsDuplicateLocationException;
import com.practica.excecption.EmsDuplicatePersonException;
import com.practica.excecption.EmsInvalidNumberOfDataException;
import com.practica.excecption.EmsInvalidTypeException;
import com.practica.excecption.EmsLocalizationNotFoundException;
import com.practica.excecption.EmsPersonNotFoundException;
import com.practica.genericas.Constantes;
import com.practica.genericas.Coordenada;
import com.practica.genericas.FechaHora;
import com.practica.genericas.Persona;
import com.practica.genericas.PosicionPersona;
import com.practica.lista.ListaContactos;

public class ContactosCovid {
    private Poblacion poblacion;
    private Localizacion localizacion;
    private ListaContactos listaContactos;

    public ContactosCovid() {
        this.poblacion = new Poblacion();
        this.localizacion = new Localizacion();
        this.listaContactos = new ListaContactos();
    }

    public Poblacion getPoblacion() {
        return poblacion;
    }

    public void setPoblacion(Poblacion poblacion) {
        this.poblacion = poblacion;
    }

    public Localizacion getLocalizacion() {
        return localizacion;
    }

    public void setLocalizacion(Localizacion localizacion) {
        this.localizacion = localizacion;
    }


    public ListaContactos getListaContactos() {
        return listaContactos;
    }

    public void setListaContactos(ListaContactos listaContactos) {
        this.listaContactos = listaContactos;
    }

    public void loadData(String data, boolean reset) throws EmsInvalidTypeException, EmsInvalidNumberOfDataException,
            EmsDuplicatePersonException, EmsDuplicateLocationException {
        // borro información anterior
        if (reset) {
            this.poblacion = new Poblacion();
            this.localizacion = new Localizacion();
            this.listaContactos = new ListaContactos();
        }
        String datas[] = dividirEntrada(data);
        for (String linea : datas) {
            String datos[] = this.dividirLineaData(linea);
            if (!datos[0].equals("PERSONA") && !datos[0].equals("LOCALIZACION")) {
                throw new EmsInvalidTypeException();
            }
            if (datos[0].equals("PERSONA")) {
                if (datos.length != Constantes.MAX_DATOS_PERSONA) {
                    throw new EmsInvalidNumberOfDataException("El número de datos para PERSONA es menor de 8");
                }
                this.poblacion.addPersona(this.crearPersona(datos));
            }
            if (datos[0].equals("LOCALIZACION")) {
                if (datos.length != Constantes.MAX_DATOS_LOCALIZACION) {
                    throw new EmsInvalidNumberOfDataException("El número de datos para LOCALIZACION es menor de 6");
                }
                PosicionPersona pp = this.crearPosicionPersona(datos);
                this.localizacion.addLocalizacion(pp);
                this.listaContactos.insertarNodoTemporal(pp);
            }
        }
    }


    @SuppressWarnings("resource")
    public void loadDataFile(String fichero, boolean reset) throws IOException, EmsInvalidNumberOfDataException, EmsDuplicateLocationException, EmsInvalidTypeException, EmsDuplicatePersonException {
        FileReader fr = null;
        try {
            // Apertura del fichero y creacion de BufferedReader para poder
            // hacer una lectura comoda (disponer del metodo readLine()).
            File archivo = new File(fichero);
            fr = new FileReader(archivo);
            BufferedReader br = new BufferedReader(fr);
            leerFichero(br, reset);
        } finally {
			/*En el finally cerramos el fichero, para asegurarnos
			que se cierra tanto si todo va bien como si salta
			una excepcion.*/
            try {
                if (null != fr) {
                    fr.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    private void leerFichero(BufferedReader br, boolean reset) throws EmsInvalidNumberOfDataException, EmsDuplicateLocationException, EmsInvalidTypeException, EmsDuplicatePersonException, IOException {
        String data;
        while ((data = br.readLine()) != null) {
            loadData(data, reset);
        }
    }

    public int findPersona(String documento) throws EmsPersonNotFoundException {
        int pos;
        try {
            pos = this.poblacion.findPersona(documento);
            return pos;
        } catch (EmsPersonNotFoundException e) {
            throw new EmsPersonNotFoundException();
        }
    }

    public int findLocalizacion(String documento, String fecha, String hora) throws EmsLocalizationNotFoundException {

        int pos;
        try {
            pos = localizacion.findLocalizacion(documento, fecha, hora);
            return pos;
        } catch (EmsLocalizationNotFoundException e) {
            throw new EmsLocalizationNotFoundException();
        }
    }

    public List<PosicionPersona> localizacionPersona(String documento) throws EmsPersonNotFoundException {
        int cont = 0;
        List<PosicionPersona> lista = new ArrayList<PosicionPersona>();
        Iterator<PosicionPersona> it = this.localizacion.getLista().iterator();
        while (it.hasNext()) {
            PosicionPersona pp = it.next();
            if (pp.getDocumento().equals(documento)) {
                cont++;
                lista.add(pp);
            }
        }
        if (cont == 0)
            throw new EmsPersonNotFoundException();
        else
            return lista;
    }

    public boolean delPersona(String documento) throws EmsPersonNotFoundException {
        int cont = 0, pos = -1;
        Iterator<Persona> it = this.poblacion.getLista().iterator();
        while (it.hasNext()) {
            Persona persona = it.next();
            if (persona.getDocumento().equals(documento)) {
                pos = cont;
            }
            cont++;
        }
        if (pos == -1) {
            throw new EmsPersonNotFoundException();
        }
        this.poblacion.getLista().remove(pos);
        return false;
    }

    private String[] dividirEntrada(String input) {
        String cadenas[] = input.split("\\n");
        return cadenas;
    }

    private String[] dividirLineaData(String data) {
        String cadenas[] = data.split("\\;");
        return cadenas;
    }

    private Persona crearPersona(String[] data) {
        Persona persona = new Persona();
        persona.setDocumento(data[1]);
        persona.setNombre(data[2]);
        persona.setApellidos(data[3]);
        persona.setEmail(data[4]);
        persona.setDireccion(data[5]);
        persona.setCp(data[6]);
        persona.setFechaNacimiento(parsearFecha(data[7]));

        return persona;
    }

    private PosicionPersona crearPosicionPersona(String[] data) {
        PosicionPersona posicionPersona = new PosicionPersona();
        String fecha = null, hora;
        float latitud = 0, longitud;
        posicionPersona.setDocumento(data[1]);
        fecha = data[2];
        hora = data[3];
        posicionPersona.setFechaPosicion(parsearFecha(fecha,hora));
        latitud = Float.parseFloat(data[4]);
        longitud = Float.parseFloat(data[5]);
        posicionPersona.setCoordenada(new Coordenada(latitud, longitud));
        return posicionPersona;
    }

    private FechaHora parsearFecha(String fecha) {
        int [] valores = parsearDiaMesAnio(fecha);
        return new FechaHora(valores[0], valores[1], valores[2], 0, 0);
    }

    private FechaHora parsearFecha(String fecha, String hora) {
        int [] fechaDMA = parsearDiaMesAnio(fecha);
        int [] horas = parsearHora(hora);
        return new FechaHora(fechaDMA[0],fechaDMA[1], fechaDMA[2], horas[0], horas[1]);
    }
    private int[] parsearDiaMesAnio (String fecha) {
        int [] fechaAux = new int[3];
        String[] valores = fecha.split("\\/");
        fechaAux [0] = Integer.parseInt(valores[0]);
        fechaAux [1] = Integer.parseInt(valores[1]);
        fechaAux [2] = Integer.parseInt(valores[2]);
        return fechaAux;
    }
    private int [] parsearHora (String hora) {
        int [] horaAux = new int[2];
        String [] valores = hora.split("\\:");
        horaAux [0] = Integer.parseInt(valores[0]);
        horaAux[1] = Integer.parseInt(valores[1]);
        return horaAux;
    }
}
