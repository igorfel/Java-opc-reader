/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package principal;

import Jama.Matrix;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafish.clients.opc.JOpc;
import javafish.clients.opc.component.OpcGroup;
import javafish.clients.opc.component.OpcItem;
import javafish.clients.opc.exception.ComponentNotFoundException;
import javafish.clients.opc.exception.ConnectivityException;
import javafish.clients.opc.exception.SynchReadException;
import javafish.clients.opc.exception.SynchWriteException;
import javafish.clients.opc.exception.UnableAddGroupException;
import javafish.clients.opc.exception.UnableAddItemException;
import javafish.clients.opc.exception.UnableRemoveGroupException;
import javafish.clients.opc.variant.Variant;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

/**
 *
 * @author geovani
 */
public class OPCRead extends javax.swing.JFrame {

    TimeSeries serieSP, seriePV, serieMV;
    TimeSeriesCollection dataset;
    ChartPanel myChart;
    JFreeChart grafico;

    JOpc opc;
    OpcItem tagSP, tagPV, tagMV, tagModo, tagKp, tagKi, tagKd;
    OpcGroup grupoOPC, leituraOPC;

    Date data;
    double SP, PV, MV, modo, KP, KI, KD, amplitude, eps, MVoperacao;
    boolean isConectado, isLeitura = false, flagRele = true;
    double fator = 163.83, fatorSintonia = 100, erro = 0;
    
    ArrayList<Double> y = new ArrayList<>();
    ArrayList<Double> u = new ArrayList<>();
    ArrayList<Integer> periodos = new ArrayList<>();
    int ciclos, contador, k; //variaveis relé
    
    ArrayList<Double> ModResult = new ArrayList<>();
    double resK, resTau, resTheta;
    
    /**
     * Creates new form OPCRead
     */
    public OPCRead() {
        initComponents();
        IniciarGrafico();
        ciclos = 6;
    }

    public void IniciarGrafico() {
        //Criar times series
        serieSP = new TimeSeries("SP");
        seriePV = new TimeSeries("PV");
        serieMV = new TimeSeries("MV");

        //Iniciar a coleção
        dataset = new TimeSeriesCollection();

        //Adicionar series à coleção
        dataset.addSeries(serieSP);
        dataset.addSeries(seriePV);
        dataset.addSeries(serieMV);

        //Fabricar o gráfico
        XYDataset dados = dataset;
        grafico = ChartFactory.createTimeSeriesChart("Dados OPC", "Tempo(s)", "Valores", dados);

        //Criar o painel
        myChart = new ChartPanel(grafico, true);
        myChart.setSize(painelGrafico.getWidth(), painelGrafico.getHeight());
        myChart.setVisible(true);
        painelGrafico.removeAll();
        painelGrafico.add(myChart);
        painelGrafico.repaint();
    }

    public void ConectarOPC() {
        JOpc.coInitialize();
        opc = new JOpc(txtHost.getText(), txtServerOPC.getText(), "MyOPC");

        tagSP = new OpcItem(txtTagSP.getText(), true, "");
        tagPV = new OpcItem(txtTagPV.getText(), true, "");
        tagMV = new OpcItem(txtTagMV.getText(), true, "");
        tagModo = new OpcItem(txtTagModo.getText(), true, "");
        tagKp = new OpcItem(txtTagKp.getText(), true, "");
        tagKi = new OpcItem(txtTagKi.getText(), true, "");
        tagKd = new OpcItem(txtTagKd.getText(), true, "");

        grupoOPC = new OpcGroup("Grupo 1", true, 100, 0.0f);
        grupoOPC.addItem(tagSP);
        grupoOPC.addItem(tagPV);
        grupoOPC.addItem(tagMV);
        grupoOPC.addItem(tagModo);
        grupoOPC.addItem(tagKp);
        grupoOPC.addItem(tagKi);
        grupoOPC.addItem(tagKd);
        
        opc.addGroup(grupoOPC);
        try {
            opc.connect();
            opc.registerGroups();
            
            isConectado = true;
            btnConectar.setText("Desconectar");
            lblStatus.setText("Conectado!");
            lblStatus.setForeground(Color.green);
            
            btnIniciar.setEnabled(true);
            btnIniciar.setText("Iniciar Leitura");
        } catch (ConnectivityException | UnableAddGroupException | UnableAddItemException ex) {
            lblStatus.setText("Erro de conexão com o OPC!");
            lblStatus.setForeground(Color.red);
            Logger.getLogger(OPCRead.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void DesconectarOPC() {
        try {
            opc.unregisterGroups();
            
            isConectado = false;
            btnConectar.setText("Conectar");
            lblStatus.setText("Desconectado...");
            btnIniciar.setEnabled(false);
            lblStatus.setForeground(Color.DARK_GRAY);
            
            isLeitura = false;
            btnIniciar.setText("Iniciar Leitura");

        } catch (UnableRemoveGroupException ex) {
            Logger.getLogger(OPCRead.class.getName()).log(Level.SEVERE, null, ex);
        }
        JOpc.coUninitialize();
    }

    public void LeituraTagsOPC() {
        data = new Date();

        try {
            if (opc.ping()) {
                leituraOPC = opc.synchReadGroup(grupoOPC);
            }
        } catch (ComponentNotFoundException | SynchReadException ex) {
            JOptionPane.showMessageDialog(null, "Erro na leitura do OPC!");
            Logger.getLogger(OPCRead.class.getName()).log(Level.SEVERE, null, ex);
        }

        SP = Double.parseDouble(leituraOPC.getItems().get(0).getValue().toString())/fator;
        PV = Double.parseDouble(leituraOPC.getItems().get(1).getValue().toString())/fator;
        MV = Double.parseDouble(leituraOPC.getItems().get(2).getValue().toString())/fator;
        modo = Double.parseDouble(leituraOPC.getItems().get(3).getValue().toString());
        KP = Double.parseDouble(leituraOPC.getItems().get(4).getValue().toString())/fatorSintonia;
        KI = Double.parseDouble(leituraOPC.getItems().get(5).getValue().toString())/fatorSintonia;
        KD = Double.parseDouble(leituraOPC.getItems().get(6).getValue().getString())/fatorSintonia;

        if (modo > 0) {
            btnModo.setBackground(Color.red);
            btnModo.setText("Manual");
        } else {
            btnModo.setBackground(Color.green);
            btnModo.setText("Automático");
        }

        txtSP.setText(truncate(SP));
        txtPV.setText(truncate(PV));
        txtMV.setText(truncate(MV));
        txtKp.setText(truncate(KP));
        txtKi.setText(truncate(KI));
        txtKd.setText(truncate(KD));

        serieSP.addOrUpdate(new Millisecond(data), SP);
        seriePV.addOrUpdate(new Millisecond(data), PV);
        serieMV.addOrUpdate(new Millisecond(data), MV);
        
        if(flagRele) {
            if(contador <= ciclos){
                rele();
            }else{
                pararRele();
                long t = Long.parseLong(cbTempoAmostragem.getSelectedItem().toString());
                ModResult = ModFOPDT(periodos, amplitude, eps, SP, y, u, t);
                resK = ModResult.get(0);
                resTau = ModResult.get(1);
                resTheta = ModResult.get(2);
            }
        }
    }

    public void loopLeitura() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isLeitura) {
                    LeituraTagsOPC();
                    long t = Long.parseLong(cbTempoAmostragem.getSelectedItem().toString());
                    esperar(t);
                }
                
                if(!isLeitura) {
                    try {
                        finalize();
                    } catch (Throwable ex) {
                        Logger.getLogger(OPCRead.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }).start();
    }

    public void esperar(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Logger.getLogger(OPCRead.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static String truncate(double valor) {
        DecimalFormat of = new DecimalFormat("#0.00");
        return of.format(valor);
    }
    
    public void escrever(OpcItem Tag, double valor) {
        Tag.setValue(new Variant(valor));
        try {
            opc.synchWriteItem(grupoOPC, Tag);
        } catch (ComponentNotFoundException | SynchWriteException ex) {
            Logger.getLogger(OPCRead.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void rele() {
        erro = SP-PV;
        
        if(contador == 0){
            if(erro >= 0){
                escrever(tagMV, (MVoperacao + amplitude)*fator);
            } else {
                escrever(tagMV, (MVoperacao - amplitude)*fator);
            }
        }else if(erro >= eps) {
            escrever(tagMV, (MVoperacao + amplitude)*fator);
        }else if(erro <= -eps){
            escrever(tagMV, (MVoperacao - amplitude)*fator);
        }
        
        y.add(PV);
        u.add(MV);
        
        //verifica se houve mudança no sinal
        if(k > 1){
            if(Math.round(y.get(k)) != Math.round(y.get(k-1))){
                contador++;
                periodos.add(k);
            }
        }
        
        k++;
    }

    public void pararRele() {
        escrever(tagModo, 0);
        esperar(20);
        flagRele = false;
    }
    
     public static ArrayList<Double> ModFOPDT(ArrayList<Integer> periodos, double amp, double eps, double ref, ArrayList<Double> y, ArrayList<Double> u, double Tamostragem) {
        ArrayList<Double> ParFOPDT = new ArrayList<>();
        double Tu, RefAux, Au, Ad, a, Ku, fase, k, tau, teta;
        int aux1, aux2;
        ParFOPDT.clear();

        //calculo do periodo total
        Tu = ((periodos.get(5) - periodos.get(4)) * Tamostragem) + ((periodos.get(4) - periodos.get(3)) * Tamostragem);
        aux1 = periodos.get(2);
        aux2 = periodos.get(4);
        double yi1[] = new double[aux2 - aux1 + 2];
        double yi2[] = new double[aux2 - aux1 + 2];
        double ui1[] = new double[aux2 - aux1 + 2];
        double ui2[] = new double[aux2 - aux1 + 2];
        double ti1[] = new double[aux2 - aux1 + 2];
        double ti2[] = new double[aux2 - aux1 + 2];

        RefAux = ref;
        for (int t = aux1; t <= aux2 - 1; t++)//pico de positivo
        {
            if (y.get(t - 1) >= RefAux) {
                RefAux = y.get(t - 1);
            }
        }
        Au = RefAux;//guardar pico de subida
        RefAux = ref;

        for (int t = aux1; t <= aux2 - 1; t++)//pico negativo
        {
            if (y.get(t - 1) <= RefAux) {
                RefAux = y.get(t - 1);
            }
        }
        Ad = RefAux;

        a = (Math.abs(Au) - Math.abs(Ad)) / 2;//amplitude de sa�da
        //fun��o descritiva do rel�
        Ku = (4 * amp) / (Math.PI * a);
        fase = Math.asin((eps / a)) * -1;//calcular defasagem da histerese
        //-----<Inicio do Calculo Ganho est�tico - M�etodo da Integral>---------------- 
        int i = 0;
        yi1[i] = 0;
        ui1[i] = 0;
        ti2[i] = 0;
        for (int t = aux1; t <= aux2; t++) {//la�o para colher os dados de 1 periodos completo do teste
            yi1[i + 1] = y.get(t - 1);
            yi2[i] = y.get(t - 1);

            ui1[i + 1] = u.get(t - 1);
            ui2[i] = u.get(t - 1);

            ti1[i] = (i + 1) * Tamostragem;
            ti2[i + 1] = (i + 1) * Tamostragem;
            i = i + 1;
        }
        yi2[i] = 0;
        ui2[i] = 0;
        ti1[i] = 0;

        Matrix Yi1 = new Matrix(yi1, 1);
        Matrix Yi2 = new Matrix(yi2, 1);
        Matrix Ti1 = new Matrix(ti1, 1);
        Matrix Ti2 = new Matrix(ti2, 1);

        Yi1 = Yi1.plusEquals(Yi2);
        Ti1 = Ti1.minusEquals(Ti2);
        Yi1 = Yi1.arrayTimes(Ti1).times(0.5);

        double A1 = 0;
        for (int j = 1; j < Yi1.getColumnDimension() - 1; j++) {
            A1 = A1 + Yi1.get(0, j);
        }
        Matrix Ui1 = new Matrix(ui1, 1);
        Matrix Ui2 = new Matrix(ui2, 1);

        Ui1 = Ui1.plusEquals(Ui2);
        Ui1 = Ui1.arrayTimes(Ti1).times(0.5);
        //Ui1 = Ui1.times(0.5);

        double A2 = 0;
        for (int j = 1; j < Ui1.getColumnDimension() - 1; j++) {
            A2 = A2 + Ui1.get(0, j);
        }

        k = A1 / A2;//ganho est�tico
        double delta;
        if (Ku * k < 1) {
            delta = 1;
        } else {
            delta = Math.pow((Ku * k), 2) - 1;
        }

        //----<Fim do calculo ganho est�tico da planta>-------------------------------
        tau = (Tu / (2 * Math.PI)) * Math.sqrt(delta);
        teta = (Tu / (2 * Math.PI)) * (Math.PI - Math.atan((2 * Math.PI * tau) / Tu) + fase);

        ParFOPDT.add(0, k);
        ParFOPDT.add(1, tau);
        ParFOPDT.add(2, teta);

        return ParFOPDT;
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        painelGrafico = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtServerOPC = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        txtHost = new javax.swing.JTextField();
        btnConectar = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        lblStatus = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        txtTagSP = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        txtTagPV = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        txtTagMV = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        txtTagModo = new javax.swing.JTextField();
        cbTempoAmostragem = new javax.swing.JComboBox<>();
        jLabel8 = new javax.swing.JLabel();
        btnIniciar = new javax.swing.JButton();
        btnModo = new javax.swing.JButton();
        txtTagKp = new javax.swing.JTextField();
        txtTagKi = new javax.swing.JTextField();
        txtTagKd = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        txtSP = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        txtPV = new javax.swing.JTextField();
        txtMV = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        txtKp = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        txtKi = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        txtKd = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        btnIniciarRele = new javax.swing.JButton();
        btnPararRele = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        txtAmplitude = new javax.swing.JTextField();
        txtEps = new javax.swing.JTextField();
        jPanel5 = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jLabel20 = new javax.swing.JLabel();
        cbMetodos = new javax.swing.JComboBox<>();
        jLabel21 = new javax.swing.JLabel();
        jTextField4 = new javax.swing.JTextField();
        jTextField5 = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        jTextField6 = new javax.swing.JTextField();
        jLabel23 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout painelGraficoLayout = new javax.swing.GroupLayout(painelGrafico);
        painelGrafico.setLayout(painelGraficoLayout);
        painelGraficoLayout.setHorizontalGroup(
            painelGraficoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 628, Short.MAX_VALUE)
        );
        painelGraficoLayout.setVerticalGroup(
            painelGraficoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 555, Short.MAX_VALUE)
        );

        jLabel1.setText("Servidor OPC:");

        txtServerOPC.setText("RSLinx Remote OPC Server");
        txtServerOPC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtServerOPCActionPerformed(evt);
            }
        });

        jLabel2.setText("Host:");

        txtHost.setText("localhost");

        btnConectar.setText("conectar");
        btnConectar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConectarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(btnConectar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jLabel1)
            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(txtServerOPC)
            .addComponent(txtHost)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtServerOPC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addGap(8, 8, 8)
                .addComponent(txtHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnConectar))
        );

        jLabel3.setText("Status:");

        jLabel4.setText("TagSP");

        txtTagSP.setText("[P_UNP]N7:19");
        txtTagSP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtTagSPActionPerformed(evt);
            }
        });

        jLabel5.setText("TagPV");

        txtTagPV.setText("[P_UNP]N7:59");

        jLabel6.setText("TagMV");

        txtTagMV.setText("[P_UNP]N7:20");

        jLabel7.setText("TagModo");

        txtTagModo.setText("[P_UNP]B19:0/2");

        cbTempoAmostragem.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "100", "200", "300", "500", "1000", "2000", "3000" }));
        cbTempoAmostragem.setSelectedIndex(3);
        cbTempoAmostragem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbTempoAmostragemActionPerformed(evt);
            }
        });

        jLabel8.setText("Tempo de amostragem:");

        btnIniciar.setText("Iniciar OPC");
        btnIniciar.setEnabled(false);
        btnIniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIniciarActionPerformed(evt);
            }
        });

        btnModo.setText("--");

        txtTagKp.setText("[P_UNP]N7:16");

        txtTagKi.setText("[P_UNP]N7:17");

        txtTagKd.setText("[P_UNP]N7:18");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(jLabel4)
                    .addComponent(jLabel6))
                .addGap(31, 31, 31)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtTagMV)
                    .addComponent(txtTagPV)
                    .addComponent(txtTagSP)))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(btnIniciar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnModo, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cbTempoAmostragem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addComponent(txtTagKp, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtTagKi, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtTagKd, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 23, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addComponent(txtTagModo))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(txtTagSP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(txtTagPV, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(txtTagMV, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(txtTagModo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 41, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtTagKp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtTagKi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtTagKd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbTempoAmostragem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnIniciar)
                    .addComponent(btnModo)))
        );

        jLabel11.setText("SP");

        jLabel18.setText("PV");

        jLabel19.setText("MV");

        jLabel12.setText("Kp");

        jLabel13.setText("Ki");

        jLabel14.setText("Kd");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel19)
                    .addComponent(jLabel18)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(txtSP, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                    .addComponent(txtPV, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtMV, javax.swing.GroupLayout.Alignment.LEADING))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel14)
                    .addComponent(jLabel13)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtKp, javax.swing.GroupLayout.DEFAULT_SIZE, 81, Short.MAX_VALUE)
                    .addComponent(txtKi, javax.swing.GroupLayout.DEFAULT_SIZE, 81, Short.MAX_VALUE)
                    .addComponent(txtKd)))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(txtSP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12)
                    .addComponent(txtKp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(txtPV, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13)
                    .addComponent(txtKi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19)
                    .addComponent(txtMV, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14)
                    .addComponent(txtKd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(34, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Propriedades Relé"));

        btnIniciarRele.setText("Iniciar");
        btnIniciarRele.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIniciarReleActionPerformed(evt);
            }
        });

        btnPararRele.setText("Parar");
        btnPararRele.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPararReleActionPerformed(evt);
            }
        });

        jLabel9.setText("Amplitude do rele:");

        jLabel10.setText("Esterese (eps):");

        txtAmplitude.setText("10");

        txtEps.setText("0.3");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel10)
                            .addComponent(jLabel9)))
                    .addComponent(btnIniciarRele, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(txtAmplitude)
                    .addComponent(txtEps)
                    .addComponent(btnPararRele, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, 0))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnIniciarRele, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnPararRele, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(txtAmplitude, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(txtEps, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Parâmetros Estimados"));

        jLabel15.setText("K:");

        jLabel16.setText("Tau:");

        jLabel17.setText("D");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(jLabel15)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel16)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel16)
                        .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel17)
                            .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
        );

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Sintonia"));

        buttonGroup1.add(jRadioButton1);
        jRadioButton1.setSelected(true);
        jRadioButton1.setText("PI");

        buttonGroup1.add(jRadioButton2);
        jRadioButton2.setText("PID");

        jLabel20.setText("Método");

        cbMetodos.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Método Ziegler-Nichols", "CHR", "Cohen-coon", "Integral do erro (IAE e ITAE)", "Modelo interno IMC", " " }));

        jLabel21.setText("Kp:");

        jLabel22.setText("Ti:");

        jLabel23.setText("Td:");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cbMetodos, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jRadioButton1)
                        .addGap(18, 18, 18)
                        .addComponent(jRadioButton2))
                    .addComponent(jLabel20)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel21)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel22)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel23)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButton1)
                    .addComponent(jRadioButton2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel20)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbMetodos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel22)
                        .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel23)
                            .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(35, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(painelGrafico, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(painelGrafico, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(46, 46, 46)))
                        .addGap(65, 65, 65))))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtServerOPCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtServerOPCActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtServerOPCActionPerformed

    private void cbTempoAmostragemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbTempoAmostragemActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cbTempoAmostragemActionPerformed

    private void btnConectarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConectarActionPerformed
        // TODO add your handling code here:
        if (isConectado) {
            DesconectarOPC();
        } else {
            ConectarOPC();
        }
    }//GEN-LAST:event_btnConectarActionPerformed

    private void btnIniciarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIniciarActionPerformed
        // TODO add your handling code here:
        if (isLeitura) {
            btnIniciar.setText("Iniciar Leitura");
            isLeitura = false;
        } else {
            btnIniciar.setText("Parar Leitura");
            loopLeitura();
            isLeitura = true;
        }
    }//GEN-LAST:event_btnIniciarActionPerformed

    private void txtTagSPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtTagSPActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtTagSPActionPerformed

    private void btnIniciarReleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIniciarReleActionPerformed
        escrever(tagModo, 1);
        esperar(20);
        y.clear();
        u.clear();
        k = 0;
        contador = 0;
        MVoperacao = MV;
        eps = Double.parseDouble(txtEps.getText());
        amplitude = Double.parseDouble(txtAmplitude.getText());
        flagRele = true;
    }//GEN-LAST:event_btnIniciarReleActionPerformed

    private void btnPararReleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPararReleActionPerformed
        // TODO add your handling code here:
        pararRele();
        
    }//GEN-LAST:event_btnPararReleActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(OPCRead.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(OPCRead.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(OPCRead.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(OPCRead.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new OPCRead().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnConectar;
    private javax.swing.JButton btnIniciar;
    private javax.swing.JButton btnIniciarRele;
    private javax.swing.JButton btnModo;
    private javax.swing.JButton btnPararRele;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JComboBox<String> cbMetodos;
    private javax.swing.JComboBox<String> cbTempoAmostragem;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JPanel painelGrafico;
    private javax.swing.JTextField txtAmplitude;
    private javax.swing.JTextField txtEps;
    private javax.swing.JTextField txtHost;
    private javax.swing.JTextField txtKd;
    private javax.swing.JTextField txtKi;
    private javax.swing.JTextField txtKp;
    private javax.swing.JTextField txtMV;
    private javax.swing.JTextField txtPV;
    private javax.swing.JTextField txtSP;
    private javax.swing.JTextField txtServerOPC;
    private javax.swing.JTextField txtTagKd;
    private javax.swing.JTextField txtTagKi;
    private javax.swing.JTextField txtTagKp;
    private javax.swing.JTextField txtTagMV;
    private javax.swing.JTextField txtTagModo;
    private javax.swing.JTextField txtTagPV;
    private javax.swing.JTextField txtTagSP;
    // End of variables declaration//GEN-END:variables
}
