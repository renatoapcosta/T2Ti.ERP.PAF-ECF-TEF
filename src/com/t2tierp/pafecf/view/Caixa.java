/**
 * <p>Title: T2Ti ERP</p>
 * <p>Description: PAF-ECF + TEF - Tela do Caixa.</p>
 *
 * <p>The MIT License</p>
 *
 * <p>Copyright: Copyright (C) 2010 T2Ti.COM</p>
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 *       The author may be contacted at:
 *           t2ti.com@gmail.com</p>
 *
 * @author Albert Eije (T2Ti.COM)
 * @version 1.0
 */
package com.t2tierp.pafecf.view;

import com.t2tierp.pafecf.controller.ConfiguracaoController;
import com.t2tierp.pafecf.controller.DAVController;
import com.t2tierp.pafecf.controller.MovimentoController;
import com.t2tierp.pafecf.controller.PreVendaController;
import com.t2tierp.pafecf.controller.ProdutoController;
import com.t2tierp.pafecf.controller.VendaController;
import com.t2tierp.pafecf.controller.VendedorController;
import com.t2tierp.pafecf.infra.Biblioteca;
import com.t2tierp.pafecf.infra.Ecf;
import com.t2tierp.pafecf.infra.Paf;
import jACBr.ACBrECF;
import jACBr.EstadoECF;
import com.t2tierp.pafecf.vo.ClienteVO;
import com.t2tierp.pafecf.vo.ConfiguracaoVO;
import com.t2tierp.pafecf.vo.DAVDetalheVO;
import com.t2tierp.pafecf.vo.FuncionarioVO;
import com.t2tierp.pafecf.vo.MovimentoVO;
import com.t2tierp.pafecf.vo.PosicaoComponentesVO;
import com.t2tierp.pafecf.vo.PreVendaDetalheVO;
import com.t2tierp.pafecf.vo.ProdutoVO;
import com.t2tierp.pafecf.vo.SangriaVO;
import com.t2tierp.pafecf.vo.SuprimentoVO;
import com.t2tierp.pafecf.vo.VendaCabecalhoVO;
import com.t2tierp.pafecf.vo.VendaDetalheVO;
import java.awt.AWTKeyStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Caixa extends javax.swing.JFrame implements MouseListener, EstadoECF {

//****************************************************************************//
// Variáveis de instância                                                     //
//****************************************************************************//
    private static int menuAberto = 0; // 0-não | 1-sim
    private static int statusCaixa = 0; // 0-aberto | 1-venda em andamento | 2-venda em recuperação ou importação de PV/DAV
    private static int itemCupom = 0;
    private static Double subTotal, totalGeral = 0.0;
    private DefaultListModel modelMenuPrincipal;
    private DefaultListModel modelMenuOperacoes;
    private DefaultListModel modelMenuFiscal;
    private DefaultListModel modelBobina;
    private DefaultListModel modelSubMenuGerente;
    private DefaultListModel modelSubMenuSupervisor;
    public static String MD5;
    public static ConfiguracaoVO configuracao;
    public static MovimentoVO movimento;
    public static ClienteVO cliente;
    public static VendaCabecalhoVO vendaCabecalho;
    public static ACBrECF ACBrECF;
    private VendaController vendaControl;
    private VendaDetalheVO vendaDetalhe;
    private ProdutoVO produto;
    private ArrayList<VendaDetalheVO> listaVendaDetalhe;
    private NumberFormat formatter = new DecimalFormat("0.00");
    private Thread bannerRotativo;

//****************************************************************************//
// Construtor                                                                 //
//****************************************************************************//
    @SuppressWarnings("LeakingThisInConstructor")
    public Caixa() {
        initComponents();

        configuracao = new ConfiguracaoController().pegaConfiguracao();

        try {
            ACBrECF = new ACBrECF();
        } catch (Throwable t) {
            JOptionPane.showMessageDialog(rootPane, t.getMessage(), "Erro do Sistema", JOptionPane.ERROR_MESSAGE);
        }
        configuraACBr();
        verificaEstadoImpressora();

        if (!Paf.ECFAutorizado()) {
            JOptionPane.showMessageDialog(rootPane, "ECF não autorizado - aplicação será encerrada.", "Aviso do Sistema", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        MD5 = Paf.geraMD5();

        movimento = new MovimentoController().verificaMovimento();
        if (movimento != null) {
            MovimentoAberto ma = new MovimentoAberto(this, true);
            ma.setLocationRelativeTo(null);
            ma.setVisible(true);
        } else {
            IniciaMovimento im = new IniciaMovimento(this, true);
            im.setLocationRelativeTo(null);
            im.setVisible(true);
        }
        menuAberto = 0;

        vendaControl = new VendaController();

        labelImagemTela.setIcon(new javax.swing.ImageIcon(getClass().getResource(configuracao.getCaminhoImagensLayout() + configuracao.getResolucaoVO().getImagemTela())));
        modelBobina = new DefaultListModel();
        bobina.setModel(modelBobina);

        setResolucao(this);
        telaPadrao();

        labelTitulo.setText(configuracao.getTituloTelaCaixa());

        definirMenuPrincipal();
        definirMenuOperacoes();
        definirMenuFiscal();
        definirSubMenuGerente();
        definirSubMenuSupervisor();

        panelF1.addMouseListener(this);
        panelF2.addMouseListener(this);
        panelF3.addMouseListener(this);
        panelF4.addMouseListener(this);
        panelF5.addMouseListener(this);
        panelF6.addMouseListener(this);
        panelF7.addMouseListener(this);
        panelF8.addMouseListener(this);
        panelF9.addMouseListener(this);
        panelF10.addMouseListener(this);
        panelF11.addMouseListener(this);
        panelF12.addMouseListener(this);

        F1Action f1Action = new F1Action();
        panelF1.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "F1Action");
        panelF1.getActionMap().put("F1Action", f1Action);

        F2Action f2Action = new F2Action();
        panelF2.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "F2Action");
        panelF2.getActionMap().put("F2Action", f2Action);

        F3Action f3Action = new F3Action();
        panelF3.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "F3Action");
        panelF3.getActionMap().put("F3Action", f3Action);

        F4Action f4Action = new F4Action();
        panelF4.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "F4Action");
        panelF4.getActionMap().put("F4Action", f4Action);

        F5Action f5Action = new F5Action();
        panelF5.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "F5Action");
        panelF5.getActionMap().put("F5Action", f5Action);

        F6Action f6Action = new F6Action();
        panelF6.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "F6Action");
        panelF6.getActionMap().put("F6Action", f6Action);

        F7Action f7Action = new F7Action();
        panelF7.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), "F7Action");
        panelF7.getActionMap().put("F7Action", f7Action);

        F8Action f8Action = new F8Action();
        panelF8.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "F8Action");
        panelF8.getActionMap().put("F8Action", f8Action);

        F9Action f9Action = new F9Action();
        panelF9.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), "F9Action");
        panelF9.getActionMap().put("F9Action", f9Action);

        F10Action f10Action = new F10Action();
        panelF10.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0), "F10Action");
        panelF10.getActionMap().put("F10Action", f10Action);

        F11Action f11Action = new F11Action();
        panelF11.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "F11Action");
        panelF11.getActionMap().put("F11Action", f11Action);

        F12Action f12Action = new F12Action();
        panelF12.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), "F12Action");
        panelF12.getActionMap().put("F12Action", f12Action);

        ESCAction escAction = new ESCAction();
        containerPrincipal.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ESCAction");
        containerPrincipal.getActionMap().put("ESCAction", escAction);

        EnterAction enterAction = new EnterAction();
        containerPrincipal.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "EnterAction");
        containerPrincipal.getActionMap().put("EnterAction", enterAction);

        SetaAcimaAction setaAcimaAction = new SetaAcimaAction();
        containerPrincipal.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true), "SetaAcimaAction");
        containerPrincipal.getActionMap().put("SetaAcimaAction", setaAcimaAction);

        SetaAbaixoAction setaAbaixoAction = new SetaAbaixoAction();
        containerPrincipal.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), "SetaAbaixoAction");
        containerPrincipal.getActionMap().put("SetaAbaixoAction", setaAbaixoAction);

        if (movimento != null) {
            labelCaixa.setText("Terminal: " + movimento.getNomeCaixa());
            labelOperador.setText("Operador: " + movimento.getLoginOperador());
        } else {
            labelCaixa.setText("");
            labelOperador.setText("");
        }

        verificaVendaAberta();

        //troca TAB por ENTER para os edits de código e quantidade
        HashSet conj = new HashSet(this.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        conj.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_ENTER, 0));
        editCodigo.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, conj);
        editQuantidade.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, conj);

        this.setPreferredSize(new Dimension(configuracao.getResolucaoVO().getLargura(), configuracao.getResolucaoVO().getAltura()));
        containerPrincipal.setPreferredSize(new Dimension(configuracao.getResolucaoVO().getLargura(), configuracao.getResolucaoVO().getAltura()));
        labelImagemTela.setBounds(0, 0, configuracao.getResolucaoVO().getLargura(), configuracao.getResolucaoVO().getAltura());
        this.pack();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        containerPrincipal = new javax.swing.JLayeredPane();
        labelTitulo = new javax.swing.JLabel();
        labelOperador = new javax.swing.JLabel();
        labelCaixa = new javax.swing.JLabel();
        panelBotoes = new javax.swing.JPanel();
        panelF1 = new javax.swing.JPanel();
        labelF1 = new javax.swing.JLabel();
        panelF2 = new javax.swing.JPanel();
        labelF2 = new javax.swing.JLabel();
        panelF3 = new javax.swing.JPanel();
        labelF3 = new javax.swing.JLabel();
        panelF4 = new javax.swing.JPanel();
        labelF4 = new javax.swing.JLabel();
        panelF5 = new javax.swing.JPanel();
        labelF5 = new javax.swing.JLabel();
        panelF6 = new javax.swing.JPanel();
        labelF6 = new javax.swing.JLabel();
        panelF7 = new javax.swing.JPanel();
        labelF7 = new javax.swing.JLabel();
        panelF8 = new javax.swing.JPanel();
        labelF8 = new javax.swing.JLabel();
        panelF9 = new javax.swing.JPanel();
        labelF9 = new javax.swing.JLabel();
        panelF10 = new javax.swing.JPanel();
        labelF10 = new javax.swing.JLabel();
        panelF11 = new javax.swing.JPanel();
        labelF11 = new javax.swing.JLabel();
        panelF12 = new javax.swing.JPanel();
        labelF12 = new javax.swing.JLabel();
        panelSubMenu = new javax.swing.JPanel();
        panelCard = new javax.swing.JPanel();
        panelSubMenuGerente = new javax.swing.JScrollPane();
        listaSubMenuGerente = new javax.swing.JList();
        panelSubMenuSupervisor = new javax.swing.JScrollPane();
        listaSubMenuSupervisor = new javax.swing.JList();
        jLabel10 = new javax.swing.JLabel();
        panelMenuPrincipal = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        listaMenuPrincipal = new javax.swing.JList();
        jLabel2 = new javax.swing.JLabel();
        panelMenuOperacoes = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        listaMenuOperacoes = new javax.swing.JList();
        jLabel5 = new javax.swing.JLabel();
        panelMenuFiscal = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        listaMenuFiscal = new javax.swing.JList();
        jLabel7 = new javax.swing.JLabel();
        panelBobina = new javax.swing.JScrollPane();
        bobina = new javax.swing.JList();
        editUnitario = new javax.swing.JFormattedTextField();
        editCodigo = new javax.swing.JFormattedTextField();
        editQuantidade = new javax.swing.JFormattedTextField();
        editSubTotal = new javax.swing.JFormattedTextField();
        editTotalItem = new javax.swing.JFormattedTextField();
        labelImagemProduto = new javax.swing.JLabel();
        labelDescricaoProduto = new javax.swing.JLabel();
        labelTotalGeral = new javax.swing.JLabel();
        labelMensagens = new javax.swing.JLabel();
        labelImagemTela = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setMinimumSize(new java.awt.Dimension(800, 600));
        setResizable(false);
        setUndecorated(true);
        getContentPane().setLayout(new java.awt.GridLayout(1, 0));

        containerPrincipal.setPreferredSize(new java.awt.Dimension(1024, 738));

        labelTitulo.setFont(new java.awt.Font("Verdana", 1, 14));
        labelTitulo.setForeground(new java.awt.Color(255, 255, 255));
        labelTitulo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelTitulo.setText("T2Ti PDV - T2Ti Tecnologia da Informação LTDA. - (61) 3042.5277");
        labelTitulo.setFocusable(false);
        labelTitulo.setName("labelTitulo"); // NOI18N
        labelTitulo.setRequestFocusEnabled(false);
        labelTitulo.setBounds(300, 10, 710, 20);
        containerPrincipal.add(labelTitulo, javax.swing.JLayeredPane.DEFAULT_LAYER);

        labelOperador.setFont(new java.awt.Font("Tahoma", 1, 11));
        labelOperador.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        labelOperador.setText("jLabel1");
        labelOperador.setBounds(754, 60, 230, 14);
        containerPrincipal.add(labelOperador, javax.swing.JLayeredPane.DEFAULT_LAYER);

        labelCaixa.setFont(new java.awt.Font("Tahoma", 1, 11));
        labelCaixa.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        labelCaixa.setText("jLabel8");
        labelCaixa.setBounds(754, 80, 230, 14);
        containerPrincipal.add(labelCaixa, javax.swing.JLayeredPane.DEFAULT_LAYER);

        panelBotoes.setBackground(new Color(255,255,255,0));
        panelBotoes.setMinimumSize(new java.awt.Dimension(950, 56));
        panelBotoes.setName("panelBotoes"); // NOI18N
        panelBotoes.setPreferredSize(new java.awt.Dimension(950, 56));
        panelBotoes.setLayout(new java.awt.GridBagLayout());

        panelF1.setBackground(new Color(255,255,255,0));
        panelF1.setToolTipText("");
        panelF1.setName("panelF1"); // NOI18N
        panelF1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        labelF1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/imgBotoes/21botaoCliente.png"))); // NOI18N
        labelF1.setText("F1 - Identifica Cliente");
        labelF1.setName("labelF1"); // NOI18N
        panelF1.add(labelF1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        panelBotoes.add(panelF1, gridBagConstraints);

        panelF2.setBackground(new Color(255,255,255,0));
        panelF2.setName("panelF2"); // NOI18N
        panelF2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        labelF2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/imgBotoes/21botaoMenuPrincipal.png"))); // NOI18N
        labelF2.setText("F2 - Menu Principal");
        labelF2.setName("labelF2"); // NOI18N
        panelF2.add(labelF2);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        panelBotoes.add(panelF2, gridBagConstraints);

        panelF3.setBackground(new Color(255,255,255,0));
        panelF3.setName("panelF3"); // NOI18N
        panelF3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        labelF3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/imgBotoes/21botaoMenuOperacoes.png"))); // NOI18N
        labelF3.setText("F3 - Menu Operações");
        labelF3.setName("labelF3"); // NOI18N
        panelF3.add(labelF3);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        panelBotoes.add(panelF3, gridBagConstraints);

        panelF4.setBackground(new Color(255,255,255,0));
        panelF4.setName("panelF4"); // NOI18N
        panelF4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        labelF4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/imgBotoes/21botaoMenuFiscal.png"))); // NOI18N
        labelF4.setText("F4 - Menu Fiscal");
        labelF4.setName("labelF4"); // NOI18N
        panelF4.add(labelF4);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        panelBotoes.add(panelF4, gridBagConstraints);

        panelF5.setBackground(new Color(255,255,255,0));
        panelF5.setName("panelF5"); // NOI18N
        panelF5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        labelF5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/imgBotoes/21botaoCalculadora.png"))); // NOI18N
        labelF5.setText("F5 - Calculadora");
        labelF5.setName("labelF5"); // NOI18N
        panelF5.add(labelF5);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        panelBotoes.add(panelF5, gridBagConstraints);

        panelF6.setBackground(new Color(255,255,255,0));
        panelF6.setName("panelF6"); // NOI18N
        panelF6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        labelF6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/imgBotoes/21botaoPesquisa.png"))); // NOI18N
        labelF6.setText("F6 - Pesquisa Produto");
        labelF6.setName("labelF6"); // NOI18N
        panelF6.add(labelF6);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        panelBotoes.add(panelF6, gridBagConstraints);

        panelF7.setBackground(new Color(255,255,255,0));
        panelF7.setName("panelF7"); // NOI18N
        panelF7.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        labelF7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/imgBotoes/21botaoEncerraVenda.png"))); // NOI18N
        labelF7.setText("F7 - Encerra Venda");
        labelF7.setName("labelF7"); // NOI18N
        panelF7.add(labelF7);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        panelBotoes.add(panelF7, gridBagConstraints);

        panelF8.setBackground(new Color(255,255,255,0));
        panelF8.setName("panelF8"); // NOI18N
        panelF8.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        labelF8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/imgBotoes/21botaoCancelaItem.png"))); // NOI18N
        labelF8.setText("F8 - Cancela Item");
        labelF8.setName("labelF8"); // NOI18N
        panelF8.add(labelF8);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        panelBotoes.add(panelF8, gridBagConstraints);

        panelF9.setBackground(new Color(255,255,255,0));
        panelF9.setName("panelF9"); // NOI18N
        panelF9.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        labelF9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/imgBotoes/21botaoCancelaCupom.png"))); // NOI18N
        labelF9.setText("F9 - Cancela Cupom");
        labelF9.setName("labelF9"); // NOI18N
        panelF9.add(labelF9);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        panelBotoes.add(panelF9, gridBagConstraints);

        panelF10.setBackground(new Color(255,255,255,0));
        panelF10.setName("panelF10"); // NOI18N
        panelF10.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        labelF10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/imgBotoes/21botaoDesconto.png"))); // NOI18N
        labelF10.setText("F10 - Concede Desconto");
        labelF10.setName("labelF10"); // NOI18N
        panelF10.add(labelF10);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        panelBotoes.add(panelF10, gridBagConstraints);

        panelF11.setBackground(new Color(255,255,255,0));
        panelF11.setName("panelF11"); // NOI18N
        panelF11.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        labelF11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/imgBotoes/21botaoGaveta.png"))); // NOI18N
        labelF11.setText("F11 - Identifica Vendedor");
        labelF11.setName("labelF11"); // NOI18N
        panelF11.add(labelF11);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        panelBotoes.add(panelF11, gridBagConstraints);

        panelF12.setBackground(new Color(255,255,255,0));
        panelF12.setName("panelF12"); // NOI18N
        panelF12.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        labelF12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/imgBotoes/21botaoSair.png"))); // NOI18N
        labelF12.setText("F12 - Sai do Caixa");
        labelF12.setName("labelF12"); // NOI18N
        panelF12.add(labelF12);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        panelBotoes.add(panelF12, gridBagConstraints);

        panelBotoes.setBounds(38, 705, 950, 56);
        containerPrincipal.add(panelBotoes, javax.swing.JLayeredPane.DEFAULT_LAYER);

        panelSubMenu.setBackground(new Color(255,255,255,0));
        panelSubMenu.setName("panelSubMenu"); // NOI18N
        panelSubMenu.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        panelCard.setLayout(new java.awt.CardLayout());

        panelSubMenuGerente.setBorder(null);
        panelSubMenuGerente.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        listaSubMenuGerente.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        panelSubMenuGerente.setViewportView(listaSubMenuGerente);

        panelCard.add(panelSubMenuGerente, "cardGerente");

        panelSubMenuSupervisor.setBorder(null);
        panelSubMenuSupervisor.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        listaSubMenuSupervisor.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        panelSubMenuSupervisor.setViewportView(listaSubMenuSupervisor);

        panelCard.add(panelSubMenuSupervisor, "cardSupervisor");

        panelSubMenu.add(panelCard, new org.netbeans.lib.awtextra.AbsoluteConstraints(8, 22, 450, 180));

        jLabel10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/layout/SubMenu.png"))); // NOI18N
        panelSubMenu.add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        panelSubMenu.setBounds(10, 280, 467, 212);
        containerPrincipal.add(panelSubMenu, javax.swing.JLayeredPane.DEFAULT_LAYER);

        panelMenuPrincipal.setBackground(new Color(255,255,255,0));
        panelMenuPrincipal.setName("panelMenuPrincipal"); // NOI18N
        panelMenuPrincipal.setPreferredSize(new java.awt.Dimension(213, 200));
        panelMenuPrincipal.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 16));
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Menu Principal");
        panelMenuPrincipal.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 8, 190, -1));

        jScrollPane2.setBorder(null);
        jScrollPane2.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        listaMenuPrincipal.setFont(new java.awt.Font("Tahoma", 1, 12));
        listaMenuPrincipal.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        listaMenuPrincipal.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listaMenuPrincipal.setAutoscrolls(false);
        jScrollPane2.setViewportView(listaMenuPrincipal);

        panelMenuPrincipal.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(7, 35, 200, 160));

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/layout/Menu.png"))); // NOI18N
        panelMenuPrincipal.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        panelMenuPrincipal.setBounds(685, 40, 213, 200);
        containerPrincipal.add(panelMenuPrincipal, javax.swing.JLayeredPane.DEFAULT_LAYER);

        panelMenuOperacoes.setBackground(new Color(255,255,255,0));
        panelMenuOperacoes.setName("panelMenuOperacoes"); // NOI18N
        panelMenuOperacoes.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 16));
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("Menu Operações");
        panelMenuOperacoes.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 8, 190, -1));

        jScrollPane4.setBorder(null);
        jScrollPane4.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        listaMenuOperacoes.setFont(new java.awt.Font("Tahoma", 1, 12));
        listaMenuOperacoes.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        listaMenuOperacoes.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listaMenuOperacoes.setAutoscrolls(false);
        jScrollPane4.setViewportView(listaMenuOperacoes);

        panelMenuOperacoes.add(jScrollPane4, new org.netbeans.lib.awtextra.AbsoluteConstraints(7, 35, 200, 160));

        jLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/layout/Menu.png"))); // NOI18N
        panelMenuOperacoes.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        panelMenuOperacoes.setBounds(715, 40, 213, 200);
        containerPrincipal.add(panelMenuOperacoes, javax.swing.JLayeredPane.DEFAULT_LAYER);

        panelMenuFiscal.setBackground(new Color(255,255,255,0));
        panelMenuFiscal.setName("panelMenuFiscal"); // NOI18N
        panelMenuFiscal.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel6.setFont(new java.awt.Font("Tahoma", 1, 16));
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("Menu Fiscal");
        panelMenuFiscal.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 8, 190, -1));

        jScrollPane5.setBorder(null);
        jScrollPane5.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        listaMenuFiscal.setFont(new java.awt.Font("Tahoma", 1, 12));
        listaMenuFiscal.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        listaMenuFiscal.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listaMenuFiscal.setAutoscrolls(false);
        listaMenuFiscal.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jScrollPane5.setViewportView(listaMenuFiscal);

        panelMenuFiscal.add(jScrollPane5, new org.netbeans.lib.awtextra.AbsoluteConstraints(7, 35, 200, 160));

        jLabel7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/layout/Menu.png"))); // NOI18N
        panelMenuFiscal.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        panelMenuFiscal.setBounds(745, 40, 213, 200);
        containerPrincipal.add(panelMenuFiscal, javax.swing.JLayeredPane.DEFAULT_LAYER);

        panelBobina.setBorder(null);
        panelBobina.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panelBobina.setName("panelBobina"); // NOI18N

        bobina.setBackground(new java.awt.Color(255, 253, 228));
        bobina.setFont(new java.awt.Font("Courier New", 1, 14));
        bobina.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        bobina.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        bobina.setFocusable(false);
        bobina.setName("Bobina"); // NOI18N
        panelBobina.setViewportView(bobina);

        panelBobina.setBounds(40, 240, 405, 360);
        containerPrincipal.add(panelBobina, javax.swing.JLayeredPane.DEFAULT_LAYER);

        editUnitario.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.background"));
        editUnitario.setBorder(null);
        editUnitario.setEditable(false);
        editUnitario.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        editUnitario.setText("0,00");
        editUnitario.setFocusable(false);
        editUnitario.setFont(new java.awt.Font("Verdana", 1, 18));
        editUnitario.setName("editUnitario"); // NOI18N
        editUnitario.setBounds(490, 462, 200, 30);
        containerPrincipal.add(editUnitario, javax.swing.JLayeredPane.DEFAULT_LAYER);

        editCodigo.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.background"));
        editCodigo.setBorder(null);
        editCodigo.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        editCodigo.setText("0");
        editCodigo.setFont(new java.awt.Font("Verdana", 1, 18));
        editCodigo.setName("editCodigo"); // NOI18N
        editCodigo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                editCodigoFocusLost(evt);
            }
        });
        editCodigo.setBounds(490, 262, 200, 30);
        containerPrincipal.add(editCodigo, javax.swing.JLayeredPane.DEFAULT_LAYER);

        editQuantidade.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.background"));
        editQuantidade.setBorder(null);
        editQuantidade.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        editQuantidade.setText("0");
        editQuantidade.setFont(new java.awt.Font("Verdana", 1, 18));
        editQuantidade.setName("editQuantidade"); // NOI18N
        editQuantidade.setBounds(490, 362, 200, 30);
        containerPrincipal.add(editQuantidade, javax.swing.JLayeredPane.DEFAULT_LAYER);

        editSubTotal.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.background"));
        editSubTotal.setBorder(null);
        editSubTotal.setEditable(false);
        editSubTotal.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        editSubTotal.setText("0,00");
        editSubTotal.setFocusable(false);
        editSubTotal.setFont(new java.awt.Font("Verdana", 1, 18));
        editSubTotal.setName("editSubTotal"); // NOI18N
        editSubTotal.setBounds(730, 562, 250, 30);
        containerPrincipal.add(editSubTotal, javax.swing.JLayeredPane.DEFAULT_LAYER);

        editTotalItem.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.background"));
        editTotalItem.setBorder(null);
        editTotalItem.setEditable(false);
        editTotalItem.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        editTotalItem.setText("0,00");
        editTotalItem.setFocusable(false);
        editTotalItem.setFont(new java.awt.Font("Verdana", 1, 18));
        editTotalItem.setName("editTotalItem"); // NOI18N
        editTotalItem.setBounds(490, 562, 200, 30);
        containerPrincipal.add(editTotalItem, javax.swing.JLayeredPane.DEFAULT_LAYER);

        labelImagemProduto.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelImagemProduto.setFocusable(false);
        labelImagemProduto.setName("imageProduto"); // NOI18N
        labelImagemProduto.setBounds(730, 250, 250, 250);
        containerPrincipal.add(labelImagemProduto, javax.swing.JLayeredPane.DEFAULT_LAYER);

        labelDescricaoProduto.setFont(new java.awt.Font("Verdana", 1, 48));
        labelDescricaoProduto.setForeground(new java.awt.Color(255, 255, 255));
        labelDescricaoProduto.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelDescricaoProduto.setText("Produto para venda");
        labelDescricaoProduto.setName("labelDescricaoProduto"); // NOI18N
        labelDescricaoProduto.setBounds(40, 110, 945, 83);
        containerPrincipal.add(labelDescricaoProduto, javax.swing.JLayeredPane.DEFAULT_LAYER);

        labelTotalGeral.setFont(new java.awt.Font("Verdana", 1, 27));
        labelTotalGeral.setForeground(new java.awt.Color(255, 255, 255));
        labelTotalGeral.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        labelTotalGeral.setText("2.785.565,44");
        labelTotalGeral.setFocusable(false);
        labelTotalGeral.setName("labelTotalGeral"); // NOI18N
        labelTotalGeral.setBounds(40, 652, 400, 40);
        containerPrincipal.add(labelTotalGeral, javax.swing.JLayeredPane.DEFAULT_LAYER);

        labelMensagens.setFont(new java.awt.Font("Verdana", 1, 18));
        labelMensagens.setForeground(new java.awt.Color(255, 255, 0));
        labelMensagens.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelMensagens.setText("<html>Mensagem grande mensagem grande mensagem grande mensagem grande</html>");
        labelMensagens.setFocusable(false);
        labelMensagens.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        labelMensagens.setName("labelMensagens"); // NOI18N
        labelMensagens.setPreferredSize(new java.awt.Dimension(772, 20));
        labelMensagens.setBounds(485, 650, 500, 45);
        containerPrincipal.add(labelMensagens, javax.swing.JLayeredPane.DEFAULT_LAYER);

        labelImagemTela.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/layout/Tela1024x768.jpg"))); // NOI18N
        labelImagemTela.setFocusable(false);
        labelImagemTela.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        labelImagemTela.setName("labelImagemTela"); // NOI18N
        labelImagemTela.setRequestFocusEnabled(false);
        labelImagemTela.setBounds(0, 0, 1024, 768);
        containerPrincipal.add(labelImagemTela, javax.swing.JLayeredPane.DEFAULT_LAYER);

        getContentPane().add(containerPrincipal);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void editCodigoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_editCodigoFocusLost
        editCodigoProdutoPerdeFoco();
    }//GEN-LAST:event_editCodigoFocusLost

// ***************************************************************************//
// Metodos principais e de infra                                              //
// ***************************************************************************//
    public static void main(String args[]) {
        //seta o look and feel para o do SO
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                Caixa c = new Caixa();
                c.setVisible(true);
            }
        });
    }

    private void sair() {
        if (statusCaixa == 0) {
            String[] opcoes = {"Sim", "Não"};
            int escolha = JOptionPane.showOptionDialog(null, "Deseja sair do sistema?", "Pergunta do Sistema",
                    JOptionPane.YES_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, opcoes, null);
            if (escolha == 0) {
                System.exit(0);
            }
        } else {
            JOptionPane.showMessageDialog(rootPane, "Existe uma venda em andamento.", "Aviso do Sistema", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void configuraACBr() {
        try {
            ACBrECF.setPorta(configuracao.getPortaECF());
            ACBrECF.setTimeOut(configuracao.getTimeOutECF());
            ACBrECF.setIntervaloAposComando(configuracao.getIntervaloECF());
            ACBrECF.setModelo(Integer.valueOf(configuracao.getImpressoraVO().getModeloACBr()));
            ACBrECF.ativar();
            ACBrECF.carregaAliquotas();
            ACBrECF.carregaFormasPagamento();
        } catch (Throwable t) {
            JOptionPane.showMessageDialog(rootPane, t.getMessage(), "Erro do Sistema", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void verificaEstadoImpressora() {
        try {
            Integer estado = ACBrECF.getEstado();
            if (estado == NAO_INICIALIZADA || estado == DESCONHECIDO) {
                JOptionPane.showMessageDialog(rootPane, "Problemas com a porta serial - aplicação será encerrada.", "Aviso do Sistema", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
            if (estado == VENDA || estado == PAGAMENTO) {
                JOptionPane.showMessageDialog(rootPane, "Existe uma venda em andamento. Cupom fiscal será cancelado.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
                // TODO : Podemos cancelar um cupom aqui?
                Ecf.cancelaCupom();
            }
            if (estado == REQUER_X) {
                String[] opcoes = {"Sim", "Não"};
                int escolha = JOptionPane.showOptionDialog(null, "É necessário emitir uma Leitura X. Deseja fazer isso agora?", "Pergunta do Sistema",
                        JOptionPane.YES_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, opcoes, null);
                if (escolha == 0) {
                    Ecf.leituraX();
                }
            }
            if (estado == REQUER_Z) {
                String[] opcoes = {"Sim", "Não"};
                int escolha = JOptionPane.showOptionDialog(null, "É necessário emitir uma Redução Z. Deseja fazer isso agora?", "Pergunta do Sistema",
                        JOptionPane.YES_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, opcoes, null);
                if (escolha == 0) {
                    Ecf.reducaoZ();
                }
            }
        } catch (Throwable t) {
            JOptionPane.showMessageDialog(rootPane, t.getMessage(), "Erro do Sistema", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setResolucao(Container container) {
        String nomeFonte = "";
        int estiloFonte = 0;
        //
        ConfiguracaoController configuracaoControl = new ConfiguracaoController();
        List<PosicaoComponentesVO> listaPosicoes = new ArrayList<PosicaoComponentesVO>();
        PosicaoComponentesVO posicaoComponente = new PosicaoComponentesVO();
        listaPosicoes = configuracaoControl.verificaPosicaoTamanho();
        String nomeComponente = "";

        for (Component componente : container.getComponents()) {
            nomeComponente = componente.getName();
            if (nomeComponente != null) {
                for (int i = 0; i < listaPosicoes.size(); i++) {
                    posicaoComponente = listaPosicoes.get(i);
                    if (posicaoComponente.getNomeComponente().equals(nomeComponente)) {
                        ((JComponent) componente).setBounds(posicaoComponente.getEsquerda(), posicaoComponente.getTopo(), posicaoComponente.getLargura(), posicaoComponente.getAltura());
                        if (posicaoComponente.getTamanhoFonte() > 0) {
                            nomeFonte = ((JComponent) componente).getFont().getName();
                            estiloFonte = ((JComponent) componente).getFont().getStyle();
                            ((JComponent) componente).setFont(new Font(nomeFonte, estiloFonte, posicaoComponente.getTamanhoFonte()));
                        }
                        if (componente instanceof JLabel) {
                            ((JLabel) componente).setText(posicaoComponente.getTextoComponente());
                        }
                        break;
                    }
                }
            }
            setResolucao((Container) componente);
        }
    }

    private void verificaVendaAberta() {
        listaVendaDetalhe = (ArrayList) vendaControl.vendaAberta();
        if (listaVendaDetalhe != null) {
            if (listaVendaDetalhe.get(0).getIdentificacaoCliente() != null) {
                Ecf.abreCupom(listaVendaDetalhe.get(0).getIdentificacaoCliente());
            } else {
                Ecf.abreCupom("");
            }
            imprimeCabecalhoBobina();
            parametrosIniciaisVenda();
            statusCaixa = 2;
            vendaCabecalho = new VendaCabecalhoVO();
            vendaCabecalho.setAcrescimo(0.0);
            vendaCabecalho.setDesconto(0.0);
            vendaCabecalho.setIdMovimento(movimento.getId());
            vendaCabecalho.setId(listaVendaDetalhe.get(0).getIdVendaCabecalho());
            labelMensagens.setText("Venda recuperada em andamento...");

            for (int i = 0; i < listaVendaDetalhe.size(); i++) {
                vendaDetalhe = listaVendaDetalhe.get(i);
                consultaProduto(vendaDetalhe.getGTIN());
                compoeItemParaVenda();
                imprimeItemBobina();
                subTotal = subTotal + vendaDetalhe.getValorTotal();
                totalGeral = totalGeral + vendaDetalhe.getValorTotal();
                atualizaTotais();
                Ecf.vendeItem(vendaDetalhe);
            }

            editCodigo.requestFocus();
            statusCaixa = 1;
        }
    }

    private void telaPadrao() {
        if (movimento == null) {
            labelMensagens.setText("CAIXA FECHADO");
        } else {
            if (movimento.getStatusMovimento().equals("T")) {
                labelMensagens.setText("SAIDA TEMPORÁRIA");
            } else {
                labelMensagens.setText("CAIXA ABERTO");
            }
        }

        if (statusCaixa == 1) {
            labelMensagens.setText("Venda em andamento...");
        }

        editQuantidade.setText("1");
        editCodigo.setText("");
        editUnitario.setText("0.00");
        editTotalItem.setText("0.00");
        editSubTotal.setText("0.00");
        labelTotalGeral.setText("0.00");
        labelDescricaoProduto.setText("");
        modelBobina.clear();

        if (configuracao.getMarketingAtivo().equals("S")) {
            Banner b = new Banner();
            bannerRotativo = new Thread(b);
            bannerRotativo.start();
        } else {
            setarImagem("padrao.png");
        }

        panelMenuPrincipal.setVisible(false);
        panelMenuOperacoes.setVisible(false);
        panelMenuFiscal.setVisible(false);
        panelSubMenu.setVisible(false);
    }

    private void setarImagem(String nome) {
        try {
            labelImagemProduto.setIcon(new javax.swing.ImageIcon(getClass().getResource(configuracao.getCaminhoImagensProdutos() + nome)));
        } catch (Exception e) {
            labelImagemProduto.setIcon(new javax.swing.ImageIcon(getClass().getResource(configuracao.getCaminhoImagensProdutos() + "padrao.png")));
        }
    }

// ***************************************************************************//
// Métodos para definição e acionamento dos menus e submenus                  //
// ***************************************************************************//
    private void definirMenuPrincipal() {
        modelMenuPrincipal = new DefaultListModel();
        listaMenuPrincipal.setModel(modelMenuPrincipal);
        modelMenuPrincipal.addElement("Supervisor");
        modelMenuPrincipal.addElement("Gerente");
        modelMenuPrincipal.addElement("Saída Temporária");
    }

    private void definirMenuOperacoes() {
        modelMenuOperacoes = new DefaultListModel();
        listaMenuOperacoes.setModel(modelMenuOperacoes);
        modelMenuOperacoes.addElement("Carrega Pré-Venda");
        modelMenuOperacoes.addElement("Mescla Pré-Venda");
        modelMenuOperacoes.addElement("Carrega DAV (Orçamento)");
        modelMenuOperacoes.addElement("Mescla DAV");
    }

    private void definirMenuFiscal() {
        modelMenuFiscal = new DefaultListModel();
        listaMenuFiscal.setModel(modelMenuFiscal);
        modelMenuFiscal.addElement("LX - Leitura X");
        modelMenuFiscal.addElement("<html><center>LMFC - Leitura Memória <br>Fiscal Completa</center></html>");
        modelMenuFiscal.addElement("<html><center>LMFS - Leitura Memória <br>Fiscal Simplificada LMFS</center></html>");
        modelMenuFiscal.addElement("Espelho MFD");
        modelMenuFiscal.addElement("Arquivo MFD");
        modelMenuFiscal.addElement("Tabela de Produtos");
        modelMenuFiscal.addElement("Estoque");
        modelMenuFiscal.addElement("Movimento por ECF");
        modelMenuFiscal.addElement("Meios de Pagamento");
        modelMenuFiscal.addElement("DAV Emitidos");
        modelMenuFiscal.addElement("Identificação do PAF-ECF");
        modelMenuFiscal.addElement("Vendas do Período");
        modelMenuFiscal.addElement("<html><center>Tabela Índice Técnicos <br>de Produção</center></html>");
    }

    private void definirSubMenuGerente() {
        modelSubMenuGerente = new DefaultListModel();
        listaSubMenuGerente.setModel(modelSubMenuGerente);
        modelSubMenuGerente.addElement("Iniciar Movimento");
        modelSubMenuGerente.addElement("Encerrar Movimento");
        modelSubMenuGerente.addElement(" ");
        modelSubMenuGerente.addElement("Suprimento");
        modelSubMenuGerente.addElement("Sangria");
        modelSubMenuGerente.addElement(" ");
        modelSubMenuGerente.addElement("Acréscimo em Dinheiro no Cupom");
        modelSubMenuGerente.addElement("Acréscimo Percentual no Cupom");
        modelSubMenuGerente.addElement("Desconto em Dinheiro no Cupom");
        modelSubMenuGerente.addElement("Desconto Percentual no Cupom");
        modelSubMenuGerente.addElement(" ");
        modelSubMenuGerente.addElement("Redução Z");
        modelSubMenuGerente.addElement(" ");
        modelSubMenuGerente.addElement("Consultar Cliente");
        modelSubMenuGerente.addElement(" ");
        modelSubMenuGerente.addElement("Configurações Caixa");
        modelSubMenuGerente.addElement(" ");
        modelSubMenuGerente.addElement("Importar Tabelas com Dispositivo (pen-drive)");
        modelSubMenuGerente.addElement("Exportar Tabelas com Dispositivo (pen-drive)");
    }

    private void definirSubMenuSupervisor() {
        modelSubMenuSupervisor = new DefaultListModel();
        listaSubMenuSupervisor.setModel(modelSubMenuSupervisor);
        modelSubMenuSupervisor.addElement("Iniciar Movimento");
        modelSubMenuSupervisor.addElement("Encerrar Movimento");
        modelSubMenuSupervisor.addElement(" ");
        modelSubMenuSupervisor.addElement("Suprimento");
        modelSubMenuSupervisor.addElement("Sangria");
        modelSubMenuSupervisor.addElement(" ");
        modelSubMenuSupervisor.addElement("Acréscimo em Dinheiro no Cupom");
        modelSubMenuSupervisor.addElement("Acréscimo Percentual no Cupom");
        modelSubMenuSupervisor.addElement("Desconto em Dinheiro no Cupom");
        modelSubMenuSupervisor.addElement("Desconto Percentual no Cupom");
        modelSubMenuSupervisor.addElement(" ");
        modelSubMenuSupervisor.addElement("Redução Z");
    }

    private void acionaMenuPrincipal() {
        if (menuAberto == 0) {
            panelMenuPrincipal.setVisible(true);
            listaMenuPrincipal.requestFocus();
            listaMenuPrincipal.setSelectedIndex(0);
            menuAberto = 1;
        }
    }

    private void acionaMenuOperacoes() {
        if (menuAberto == 0) {
            panelMenuOperacoes.setVisible(true);
            listaMenuOperacoes.requestFocus();
            listaMenuOperacoes.setSelectedIndex(0);
            menuAberto = 1;
        }
    }

    private void acionaMenuFiscal() {
        if (menuAberto == 0) {
            panelMenuFiscal.setVisible(true);
            listaMenuFiscal.requestFocus();
            listaMenuFiscal.setSelectedIndex(0);
            menuAberto = 1;
        }
    }

    private void fechaMenuOperacoes() {
        panelMenuOperacoes.setVisible(false);
        labelMensagens.setText("");
        menuAberto = 0;
    }

// ***************************************************************************//
// Actions vinculadas ao pressionamento de teclas                             //
// ***************************************************************************//
    private class F1Action extends AbstractAction {

        public F1Action() {
        }

        public void actionPerformed(ActionEvent e) {
            identificaCliente();
        }
    }

    private class F2Action extends AbstractAction {

        public F2Action() {
        }

        public void actionPerformed(ActionEvent e) {
            acionaMenuPrincipal();
        }
    }

    private class F3Action extends AbstractAction {

        public F3Action() {
        }

        public void actionPerformed(ActionEvent e) {
            acionaMenuOperacoes();
        }
    }

    private class F4Action extends AbstractAction {

        public F4Action() {
        }

        public void actionPerformed(ActionEvent e) {
            acionaMenuFiscal();
        }
    }

    private class F5Action extends AbstractAction {

        public F5Action() {
        }

        public void actionPerformed(ActionEvent e) {
            //TODO : Devemos fazer uma calculadora própria?
            try {
                Runtime.getRuntime().exec("calc.exe");
            } catch (Exception ex) {
            }
        }
    }

    private class F6Action extends AbstractAction {

        public F6Action() {
        }

        public void actionPerformed(ActionEvent e) {
            localizaProduto();
        }
    }

    private class F7Action extends AbstractAction {

        public F7Action() {
        }

        public void actionPerformed(ActionEvent e) {
            iniciaEncerramentoVenda();
        }
    }

    private class F8Action extends AbstractAction {

        public F8Action() {
        }

        public void actionPerformed(ActionEvent e) {
            cancelaItem();
        }
    }

    private class F9Action extends AbstractAction {

        public F9Action() {
        }

        public void actionPerformed(ActionEvent e) {
            cancelaCupom();
        }
    }

    private class F10Action extends AbstractAction {

        public F10Action() {
        }

        public void actionPerformed(ActionEvent e) {
            descontoValor();
        }
    }

    private class F11Action extends AbstractAction {

        public F11Action() {
        }

        public void actionPerformed(ActionEvent e) {
            identificaVendedor();
        }
    }

    private class F12Action extends AbstractAction {

        public F12Action() {
        }

        public void actionPerformed(ActionEvent e) {
            sair();
        }
    }

    private class ESCAction extends AbstractAction {

        public ESCAction() {
        }

        public void actionPerformed(ActionEvent e) {
            teclouESC();
        }
    }

    private class EnterAction extends AbstractAction {

        public EnterAction() {
        }

        public void actionPerformed(ActionEvent e) {
            teclouEnter();
        }
    }

    private class SetaAcimaAction extends AbstractAction {

        public SetaAcimaAction() {
        }

        public void actionPerformed(ActionEvent e) {
            teclouSetaAcimaAbaixo();
        }
    }

    private class SetaAbaixoAction extends AbstractAction {

        public SetaAbaixoAction() {
        }

        public void actionPerformed(ActionEvent e) {
            teclouSetaAcimaAbaixo();
        }
    }

// ***************************************************************************//
// Controle do pressionamento das teclas                                      //
// ***************************************************************************//
    private void teclouEnter() {
        //menu principal
        if (this.getFocusOwner() == listaMenuPrincipal) {
            //supervisor
            if (listaMenuPrincipal.getSelectedIndex() == 0) {
                panelSubMenu.setVisible(true);
                ((CardLayout) panelCard.getLayout()).show(panelCard, "cardSupervisor");
                listaSubMenuSupervisor.requestFocus();
                listaSubMenuSupervisor.setSelectedIndex(0);
            }
            //gerente
            if (listaMenuPrincipal.getSelectedIndex() == 1) {
                panelSubMenu.setVisible(true);
                ((CardLayout) panelCard.getLayout()).show(panelCard, "cardGerente");
                listaSubMenuGerente.requestFocus();
                listaSubMenuGerente.setSelectedIndex(0);
            }
            //saida temporaria
            if (listaMenuPrincipal.getSelectedIndex() == 2) {
                String[] opcoes = {"Sim", "Não"};
                int escolha = JOptionPane.showOptionDialog(null, "Deseja fechar o caixa temporariamente?", "Pergunta do Sistema",
                        JOptionPane.YES_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, opcoes, null);
                if (escolha == 0) {
                    MovimentoController movimentoControl = new MovimentoController();
                    movimentoControl.saidaTemporaria(movimento);
                    MovimentoAberto ma = new MovimentoAberto(this, true);
                    ma.setLocationRelativeTo(null);
                    ma.setVisible(true);
                }
            }
        }
        //menu principal - submenu supervisor
        if (this.getFocusOwner() == listaSubMenuSupervisor) {
            //inicia movimento
            if (listaSubMenuSupervisor.getSelectedIndex() == 0) {
                iniciaMovimento();
            }
            //encerra movimento
            if (listaSubMenuSupervisor.getSelectedIndex() == 1) {
                encerraMovimento();
            }
            //suprimento
            if (listaSubMenuSupervisor.getSelectedIndex() == 3) {
                suprimento();
            }
            //sangria
            if (listaSubMenuSupervisor.getSelectedIndex() == 4) {
                sangria();
            }
            //acrescimo em dinheiro no cupom
            if (listaSubMenuSupervisor.getSelectedIndex() == 6) {
                acrescimoValor();
            }
            //acrescimo percentual no cupom
            if (listaSubMenuSupervisor.getSelectedIndex() == 7) {
                acrescimoTaxa();
            }
            //desconto em dinheiro no cupom
            if (listaSubMenuSupervisor.getSelectedIndex() == 8) {
                descontoValor();
            }
            //desconto percentual no cupom
            if (listaSubMenuSupervisor.getSelectedIndex() == 9) {
                descontoTaxa();
            }
            //reducao z
            if (listaSubMenuSupervisor.getSelectedIndex() == 11) {
                String[] opcoes = {"Sim", "Não"};
                int escolha = JOptionPane.showOptionDialog(null, "Tem Certeza Que Deseja Executar a Redução Z?\nO Movimento da Impressora Será Suspenso no dia de Hoje.", "Pergunta do Sistema",
                        JOptionPane.YES_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, opcoes, null);
                if (escolha == 0) {
                    Ecf.reducaoZ();
                }
            }
        }
        //menu principal - submenu gerente
        if (this.getFocusOwner() == listaSubMenuGerente) {
            //inicia movimento
            if (listaSubMenuGerente.getSelectedIndex() == 0) {
                iniciaMovimento();
            }
            //encerra movimento
            if (listaSubMenuGerente.getSelectedIndex() == 1) {
                encerraMovimento();
            }
            //suprimento
            if (listaSubMenuGerente.getSelectedIndex() == 3) {
                suprimento();
            }
            //sangria
            if (listaSubMenuGerente.getSelectedIndex() == 4) {
                sangria();
            }
            //acrescimo em dinheiro no cupom
            if (listaSubMenuGerente.getSelectedIndex() == 6) {
                acrescimoValor();
            }
            //acrescimo percentual no cupom
            if (listaSubMenuGerente.getSelectedIndex() == 7) {
                acrescimoTaxa();
            }
            //desconto em dinheiro no cupom
            if (listaSubMenuGerente.getSelectedIndex() == 8) {
                descontoValor();
            }
            //desconto percentual no cupom
            if (listaSubMenuGerente.getSelectedIndex() == 9) {
                descontoTaxa();
            }
            //reducao z
            if (listaSubMenuGerente.getSelectedIndex() == 11) {
                String[] opcoes = {"Sim", "Não"};
                int escolha = JOptionPane.showOptionDialog(null, "Tem Certeza Que Deseja Executar a Redução Z?\nO Movimento da Impressora Será Suspenso no dia de Hoje.", "Pergunta do Sistema",
                        JOptionPane.YES_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, opcoes, null);
                if (escolha == 0) {
                    Ecf.reducaoZ();
                }
            }
            //consultar cliente
            if (listaSubMenuGerente.getSelectedIndex() == 13) {
                ImportaCliente.main(new String[1]);
            }
            //configurações
            if (listaSubMenuGerente.getSelectedIndex() == 15) {
                Configuracao.main(new String[1]);
            }
        }
        //menu operacoes
        if (this.getFocusOwner() == listaMenuOperacoes) {
            //carrega pre-venda
            if (listaMenuOperacoes.getSelectedIndex() == 0) {
                if (statusCaixa == 0) {
                    ValorInteiro janelaValorInteiro = new ValorInteiro(this, true);
                    janelaValorInteiro.setLocationRelativeTo(null);
                    janelaValorInteiro.setVisible(true);
                    Integer valor = janelaValorInteiro.retornaValor();
                    if (valor > 0) {
                        fechaMenuOperacoes();
                        carregaPreVenda(janelaValorInteiro.retornaValor());
                    }
                } else {
                    JOptionPane.showMessageDialog(rootPane, "Já existe uma venda em andamento.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
                }
            }
            //mescla pre-venda
            if (listaMenuOperacoes.getSelectedIndex() == 1) {
                if (statusCaixa == 0) {
                    mesclaPreVenda();
                } else {
                    JOptionPane.showMessageDialog(rootPane, "Já existe uma venda em andamento.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
                }
            }
            //carrega dav
            if (listaMenuOperacoes.getSelectedIndex() == 2) {
                if (statusCaixa == 0) {
                    ValorInteiro janelaValorInteiro = new ValorInteiro(this, true);
                    janelaValorInteiro.setLocationRelativeTo(null);
                    janelaValorInteiro.setVisible(true);
                    Integer valor = janelaValorInteiro.retornaValor();
                    if (valor > 0) {
                        fechaMenuOperacoes();
                        carregaDAV(janelaValorInteiro.retornaValor());
                    }
                } else {
                    JOptionPane.showMessageDialog(rootPane, "Já existe uma venda em andamento.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
                }
            }
            //mescla dav
            if (listaMenuOperacoes.getSelectedIndex() == 3) {
                if (statusCaixa == 0) {
                    mesclaDAV();
                } else {
                    JOptionPane.showMessageDialog(rootPane, "Já existe uma venda em andamento.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
        //menu fiscal
        if (this.getFocusOwner() == listaMenuFiscal) {
            //Leitura X
            if (listaMenuFiscal.getSelectedIndex() == 0) {
                String[] opcoes = {"Sim", "Não"};
                int escolha = JOptionPane.showOptionDialog(null, "Confirma a emissão da Leitura X?", "Pergunta do Sistema",
                        JOptionPane.YES_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, opcoes, null);
                if (escolha == 0) {
                    Ecf.leituraX();
                }
            }
            //LMFC
            if (listaMenuFiscal.getSelectedIndex() == 1) {
                LMFC.main(new String[1]);
            }
            //LMFS
            if (listaMenuFiscal.getSelectedIndex() == 2) {
                LMFS.main(new String[1]);
            }
            //Espelho MFD
            if (listaMenuFiscal.getSelectedIndex() == 3) {
                EspelhoMFD.main(new String[1]);
            }
            //Arquivo MFD
            if (listaMenuFiscal.getSelectedIndex() == 4) {
                ArquivoMFD.main(new String[1]);
            }
            //Tabela de Produtos
            if (listaMenuFiscal.getSelectedIndex() == 5) {
                String[] opcoes = {"Sim", "Não"};
                int escolha = JOptionPane.showOptionDialog(null, "Deseja gerar o arquivo da Tabela de Produtos?", "Pergunta do Sistema",
                        JOptionPane.YES_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, opcoes, null);
                if (escolha == 0) {
                    Paf.geraTabelaProdutos();
                }
            }
            //Estoque
            if (listaMenuFiscal.getSelectedIndex() == 6) {
                String[] opcoes = {"Sim", "Não"};
                int escolha = JOptionPane.showOptionDialog(null, "Deseja gerar o arquivo do Estoque?", "Pergunta do Sistema",
                        JOptionPane.YES_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, opcoes, null);
                if (escolha == 0) {
                    Paf.geraArquivoEstoque();
                }
            }
            //Movimento ECF
            if (listaMenuFiscal.getSelectedIndex() == 7) {
                String[] opcoes = {"Sim", "Não"};
                int escolha = JOptionPane.showOptionDialog(null, "Deseja gerar o movimento?", "Pergunta do Sistema",
                        JOptionPane.YES_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, opcoes, null);
                if (escolha == 0) {
                    Paf.geraMovimentoECF();
                }
            }
            //Meios de Pagamento
            if (listaMenuFiscal.getSelectedIndex() == 8) {
                MeiosPagamento.main(new String[1]);
            }
            //DAV Emitidos
            if (listaMenuFiscal.getSelectedIndex() == 9) {
                DAVEmitidos.main(new String[1]);
            }
            //Identificação PAF-ECF
            if (listaMenuFiscal.getSelectedIndex() == 10) {
                String[] opcoes = {"Sim", "Não"};
                int escolha = JOptionPane.showOptionDialog(null, "Deseja emitir o relatório de identificação do PAF-ECF?", "Pergunta do Sistema",
                        JOptionPane.YES_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, opcoes, null);
                if (escolha == 0) {
                    Paf.identificacaoPafEcf();
                }
            }
            //Vendas no período
            if (listaMenuFiscal.getSelectedIndex() == 11) {
                VendasPeriodo.main(new String[1]);
            }
            //Tabela de indice tecnico de produção
            //TODO : Devemos implementar essa funcionalidade?
            if (listaMenuFiscal.getSelectedIndex() == 12) {
                String[] opcoes = {"Sim", "Não"};
                int escolha = JOptionPane.showOptionDialog(null, "Devemos implementar essa funcionalidade?", "Pergunta do Sistema",
                        JOptionPane.YES_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, opcoes, null);
                if (escolha == 0) {
                    JOptionPane.showMessageDialog(rootPane, "Ponha sua opinião no EAD.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }

    }

    private void teclouESC() {
        if (this.getFocusOwner() == listaMenuPrincipal) {
            panelMenuPrincipal.setVisible(false);
            menuAberto = 0;
        }
        if (this.getFocusOwner() == listaMenuOperacoes) {
            panelMenuOperacoes.setVisible(false);
            menuAberto = 0;
        }
        if (this.getFocusOwner() == listaMenuFiscal) {
            panelMenuFiscal.setVisible(false);
            menuAberto = 0;
        }
        if (this.getFocusOwner() == listaSubMenuGerente
                || this.getFocusOwner() == listaSubMenuSupervisor) {
            listaMenuPrincipal.requestFocus();
            listaMenuPrincipal.setSelectedIndex(0);
            panelSubMenu.setVisible(false);
        }
    }

    private void teclouSetaAcimaAbaixo() {
        String selecionado = "";

        if (this.getFocusOwner() == listaMenuPrincipal) {
            selecionado = (String) modelMenuPrincipal.get(listaMenuPrincipal.getSelectedIndex());
        }

        if (this.getFocusOwner() == listaMenuOperacoes) {
            selecionado = (String) modelMenuOperacoes.get(listaMenuOperacoes.getSelectedIndex());
        }

        if (this.getFocusOwner() == listaMenuFiscal) {
            selecionado = (String) modelMenuFiscal.get(listaMenuFiscal.getSelectedIndex());
        }

        if (this.getFocusOwner() == listaSubMenuGerente) {
            selecionado = (String) modelSubMenuGerente.get(listaSubMenuGerente.getSelectedIndex());
        }

        if (this.getFocusOwner() == listaSubMenuSupervisor) {
            selecionado = (String) modelSubMenuSupervisor.get(listaSubMenuSupervisor.getSelectedIndex());
        }

        labelMensagens.setText(selecionado);
        this.repaint();
    }

// ***************************************************************************//
// Métodos referentes ao Menu Principal e seus SubMenus                       //
// ***************************************************************************//
    private void iniciaMovimento() {
        MovimentoController movimentoControl = new MovimentoController();
        movimento = movimentoControl.verificaMovimento();
        if (movimento != null) {
            JOptionPane.showMessageDialog(rootPane, "Já existe um movimento aberto.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
        } else {
            IniciaMovimento im = new IniciaMovimento(this, true);
            im.setLocationRelativeTo(null);
            im.setVisible(true);
        }
    }

    private void encerraMovimento() {
        MovimentoController movimentoControl = new MovimentoController();
        movimento = movimentoControl.verificaMovimento();
        if (movimento == null) {
            JOptionPane.showMessageDialog(rootPane, "Não existe movimento aberto.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
        } else {
            EncerraMovimento em = new EncerraMovimento(this, true);
            em.setLocationRelativeTo(null);
            em.setVisible(true);
        }
    }

    private void suprimento() {
        if (statusCaixa == 0) {
            ValorReal janelaValorReal = new ValorReal(this, true);
            janelaValorReal.setLocationRelativeTo(null);
            janelaValorReal.setVisible(true);
            Double valorSuprimento = janelaValorReal.retornaValor();
            if (valorSuprimento > 0) {
                Ecf.suprimento(valorSuprimento, configuracao.getDescricaoSuprimento());
                SuprimentoVO suprimento = new SuprimentoVO();
                suprimento.setIdMovimento(movimento.getId());
                java.util.Date data = new java.util.Date();
                java.sql.Date hoje = new java.sql.Date(data.getTime());
                suprimento.setDataSuprimento(hoje);
                suprimento.setValor(valorSuprimento);
                MovimentoController movimentoControl = new MovimentoController();
                movimentoControl.suprimento(suprimento);
                movimento.setTotalSuprimento(movimento.getTotalSuprimento() + valorSuprimento);
            }
        } else {
            JOptionPane.showMessageDialog(rootPane, "Existe uma venda em andamento.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void sangria() {
        if (statusCaixa == 0) {
            ValorReal janelaValorReal = new ValorReal(this, true);
            janelaValorReal.setLocationRelativeTo(null);
            janelaValorReal.setVisible(true);
            Double valorSangria = janelaValorReal.retornaValor();
            if (valorSangria > 0) {
                Ecf.sangria(valorSangria, configuracao.getDescricaoSangria());
                SangriaVO sangria = new SangriaVO();
                sangria.setIdMovimento(movimento.getId());
                java.util.Date data = new java.util.Date();
                java.sql.Date hoje = new java.sql.Date(data.getTime());
                sangria.setDataSangria(hoje);
                sangria.setValor(valorSangria);
                MovimentoController movimentoControl = new MovimentoController();
                movimentoControl.sangria(sangria);
                movimento.setTotalSangria(movimento.getTotalSangria() + valorSangria);
            }
        } else {
            JOptionPane.showMessageDialog(rootPane, "Existe uma venda em andamento.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void acrescimoValor() {
        if (statusCaixa == 1) {
            DescontoAcrescimo janelaDescontoAcrescimo = new DescontoAcrescimo(this, true);
            janelaDescontoAcrescimo.setLocationRelativeTo(null);
            janelaDescontoAcrescimo.setVisible(true);
            Double valor = janelaDescontoAcrescimo.retornaValor();
            if (valor > 0) {
                vendaCabecalho.setAcrescimo(janelaDescontoAcrescimo.retornaValor());
            }
        } else {
            JOptionPane.showMessageDialog(rootPane, "Não existe venda em andamento.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void acrescimoTaxa() {
        if (statusCaixa == 1) {
            DescontoAcrescimo janelaDescontoAcrescimo = new DescontoAcrescimo(this, true);
            janelaDescontoAcrescimo.setLocationRelativeTo(null);
            janelaDescontoAcrescimo.setVisible(true);
            Double valor = janelaDescontoAcrescimo.retornaValor();
            if (valor > 0) {
                vendaCabecalho.setTaxaAcrescimo(janelaDescontoAcrescimo.retornaValor());
            }
        } else {
            JOptionPane.showMessageDialog(rootPane, "Não existe venda em andamento.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void descontoValor() {
        if (statusCaixa == 1) {
            DescontoAcrescimo janelaDescontoAcrescimo = new DescontoAcrescimo(this, true);
            janelaDescontoAcrescimo.setLocationRelativeTo(null);
            janelaDescontoAcrescimo.setVisible(true);
            Double valor = janelaDescontoAcrescimo.retornaValor();
            if (valor > 0) {
                vendaCabecalho.setDesconto(janelaDescontoAcrescimo.retornaValor());
            }
        } else {
            JOptionPane.showMessageDialog(rootPane, "Não existe venda em andamento.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void descontoTaxa() {
        if (statusCaixa == 1) {
            DescontoAcrescimo janelaDescontoAcrescimo = new DescontoAcrescimo(this, true);
            janelaDescontoAcrescimo.setLocationRelativeTo(null);
            janelaDescontoAcrescimo.setVisible(true);
            Double valor = janelaDescontoAcrescimo.retornaValor();
            if (valor > 0) {
                vendaCabecalho.setTaxaDesconto(janelaDescontoAcrescimo.retornaValor());
            }
        } else {
            JOptionPane.showMessageDialog(rootPane, "Não existe venda em andamento.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
        }
    }

// ***************************************************************************//
// Métodos referentes ao Menu Operações                                       //
// ***************************************************************************//
    public void carregaPreVenda(Integer numero) {
        PreVendaDetalheVO preVendaDetalhe = new PreVendaDetalheVO();
        ProdutoController produtoControl = new ProdutoController();
        PreVendaController preVendaControl = new PreVendaController();
        List<PreVendaDetalheVO> listaPreVenda = (ArrayList) preVendaControl.carregaPreVenda(numero);
        if (listaPreVenda != null) {
            iniciaVenda();
            statusCaixa = 2;
            vendaCabecalho.setIdPreVenda(listaPreVenda.get(0).getIdPreVendaCabecalho());
            for (int i = 0; i < listaPreVenda.size(); i++) {
                preVendaDetalhe = listaPreVenda.get(i);
                produto = produtoControl.consultaId(preVendaDetalhe.getIdProduto());
                vendaDetalhe = new VendaDetalheVO();
                vendaDetalhe.setQuantidade(preVendaDetalhe.getQuantidade());
                vendaDetalhe.setValorUnitario(preVendaDetalhe.getValorUnitario());
                vendaDetalhe.setValorTotal(preVendaDetalhe.getValorTotal());
                vendeItem();
                subTotal = subTotal + vendaDetalhe.getValorTotal();
                totalGeral = totalGeral + vendaDetalhe.getValorTotal();
                atualizaTotais();
            }
            editCodigo.requestFocus();
            statusCaixa = 1;
        } else {
            JOptionPane.showMessageDialog(rootPane, "Pré-Venda inexistente ou já efetivada/mesclada.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void carregaDAV(Integer numero) {
        DAVDetalheVO DAVDetalhe = new DAVDetalheVO();
        ProdutoController produtoControl = new ProdutoController();
        DAVController DAVControl = new DAVController();
        List<DAVDetalheVO> listaDAV = (ArrayList) DAVControl.carregaDAV(numero);
        if (listaDAV != null) {
            iniciaVenda();
            statusCaixa = 2;
            vendaCabecalho.setIdDAV(listaDAV.get(0).getIdDAVCabecalho());
            for (int i = 0; i < listaDAV.size(); i++) {
                DAVDetalhe = listaDAV.get(i);
                produto = produtoControl.consultaId(DAVDetalhe.getIdProduto());
                vendaDetalhe = new VendaDetalheVO();
                vendaDetalhe.setQuantidade(DAVDetalhe.getQuantidade());
                vendaDetalhe.setValorUnitario(DAVDetalhe.getValorUnitario());
                vendaDetalhe.setValorTotal(DAVDetalhe.getValorTotal());
                vendeItem();
                subTotal = subTotal + vendaDetalhe.getValorTotal();
                totalGeral = totalGeral + vendaDetalhe.getValorTotal();
                atualizaTotais();
            }
            editCodigo.requestFocus();
            statusCaixa = 1;
        } else {
            JOptionPane.showMessageDialog(rootPane, "DAV inexistente ou já efetivado/mesclado.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void mesclaPreVenda() {
        MesclaPreVenda janelaMesclaPreVenda = new MesclaPreVenda(this, true);
        janelaMesclaPreVenda.setLocationRelativeTo(null);
        janelaMesclaPreVenda.setVisible(true);
        if (janelaMesclaPreVenda.idNovaPreVenda != null) {
            carregaPreVenda(janelaMesclaPreVenda.idNovaPreVenda);
            fechaMenuOperacoes();
        }
    }

    private void mesclaDAV() {
        MesclaDAV janelaMesclaDAV = new MesclaDAV(this, true);
        janelaMesclaDAV.setLocationRelativeTo(null);
        janelaMesclaDAV.setVisible(true);
        if (janelaMesclaDAV.idNovoDAV != null) {
            carregaDAV(janelaMesclaDAV.idNovoDAV);
            fechaMenuOperacoes();
        }
    }

// ***************************************************************************//
// Métodos para controle da venda                                             //
// ***************************************************************************//
    private void localizaProduto() {
        if (statusCaixa == 1) {
            ImportaProduto janelaImportaProduto = new ImportaProduto(this, true);
            janelaImportaProduto.setLocationRelativeTo(null);
            janelaImportaProduto.setVisible(true);
            editCodigo.setText(janelaImportaProduto.getGTIN());
        } else {
            JOptionPane.showMessageDialog(rootPane, "Não existe venda em andamento.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void identificaCliente() {
        if (movimento != null) {
            if (statusCaixa != 1) {
                IdentificaCliente.main(new String[1]);
            } else {
                JOptionPane.showMessageDialog(rootPane, "Já existe uma venda em andamento.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(rootPane, "Não existe movimento aberto.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void identificaVendedor() {
        if (statusCaixa == 1) {
            ValorInteiro janelaValorInteiro = new ValorInteiro(this, true);
            janelaValorInteiro.setLocationRelativeTo(null);
            janelaValorInteiro.setVisible(true);
            Integer valor = janelaValorInteiro.retornaValor();
            VendedorController vendedorControl = new VendedorController();
            FuncionarioVO vendedor = vendedorControl.consultaVendedor(valor);
            if (vendedor != null) {
                vendaCabecalho.setIdVendedor(vendedor.getId());
            } else {
                JOptionPane.showMessageDialog(rootPane, "Vendedor: código inválido ou inexistente.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(rootPane, "Não existe venda em andamento.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void iniciaVenda() {
        if (movimento == null) {
            JOptionPane.showMessageDialog(rootPane, "Não existe um movimento aberto.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
        } else {
            try {
                //instancia venda e detalhe
                vendaCabecalho = new VendaCabecalhoVO();
                vendaCabecalho.setAcrescimo(0.0);
                vendaCabecalho.setDesconto(0.0);
                listaVendaDetalhe = new ArrayList<VendaDetalheVO>();

                //atribui dados do cliente e abre o cupom
                if (cliente != null) {
                    vendaCabecalho.setIdCliente(cliente.getId());
                    vendaCabecalho.setNomeCliente(cliente.getNome());
                    vendaCabecalho.setCPFouCNPJCliente(cliente.getCPFOuCNPJ());
                    Ecf.abreCupom(cliente.getCPFOuCNPJ());
                } else {
                    Ecf.abreCupom("");
                }

                imprimeCabecalhoBobina();
                parametrosIniciaisVenda();
                statusCaixa = 1;
                labelMensagens.setText("Venda em andamento...");
                vendaCabecalho.setIdMovimento(movimento.getId());
                java.util.Date data = new java.util.Date();
                java.sql.Timestamp hoje = new java.sql.Timestamp(data.getTime());
                vendaCabecalho.setDataHoraVenda(hoje);
                vendaCabecalho.setStatusVenda("A");
                vendaCabecalho.setIdCFOP(configuracao.getCFOPECF());
                vendaCabecalho.setCOO(Integer.valueOf(ACBrECF.getNumCupom()));
                vendaCabecalho.setCCF(Integer.valueOf(ACBrECF.getNumCCF()));
                vendaCabecalho = vendaControl.iniciaVenda(vendaCabecalho);
                editCodigo.requestFocus();
                editCodigo.selectAll();
            } catch (Throwable t) {
                JOptionPane.showMessageDialog(rootPane, t.getMessage(), "Erro do Sistema", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void parametrosIniciaisVenda() {
        setarImagem("padrao.png");
        itemCupom = 0;
        subTotal = 0.0;
        totalGeral = 0.0;
    }

    private void imprimeCabecalhoBobina() {
        modelBobina.addElement(Biblioteca.repete("-", 48));
        modelBobina.addElement("               ** CUPOM FISCAL **               ");
        modelBobina.addElement(Biblioteca.repete("-", 48));
        modelBobina.addElement("ITEM CÓDIGO         DESCRIÇÃO                   ");
        modelBobina.addElement("QTD.     UN      VL.UNIT.(R$) ST     VL.ITEM(R$)");
        modelBobina.addElement(Biblioteca.repete("-", 48));
    }

    private void editCodigoProdutoPerdeFoco() {
        if (!editCodigo.getText().equals("")) {
            if (menuAberto == 0) {
                if (statusCaixa == 0) {
                    iniciaVenda();
                }
                if (!editCodigo.getText().equals("")) {
                    //pega dados do produto
                    consultaProduto(editCodigo.getText());
                    if (produto != null) {
                        editUnitario.setText(produto.getValorVenda().toString());
                        labelDescricaoProduto.setText(produto.getDescricaoPDV());
                        //carrega imagem do produto
                        String imagem = editCodigo.getText() + ".jpg";
                        setarImagem(imagem);
                        Double total = produto.getValorVenda() * Double.valueOf(editQuantidade.getText());
                        editTotalItem.setText(total.toString());
                        vendaDetalhe = new VendaDetalheVO();
                        vendeItem();
                        subTotal = subTotal + vendaDetalhe.getValorTotal();
                        totalGeral = totalGeral + vendaDetalhe.getValorTotal();
                        atualizaTotais();
                        editCodigo.setText("");
                        editCodigo.requestFocus();
                        editQuantidade.setText("1");
                        //formata valores para tela
                        String unitario = formatter.format(produto.getValorVenda());
                        editUnitario.setText(unitario);
                        String totalItem = formatter.format(total);
                        editTotalItem.setText(totalItem);
                    } else {
                        JOptionPane.showMessageDialog(rootPane, "Código não encontrado.", "Aviso do Sistema", JOptionPane.ERROR_MESSAGE);
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                editUnitario.setText("0,00");
                                editTotalItem.setText("0,00");
                                editQuantidade.setText("1");
                                labelDescricaoProduto.setText("");
                                editCodigo.requestFocus();
                            }
                        });
                    }
                }
            }
        }
    }

    private void consultaProduto(String codigo) {
        ProdutoController produtoControl = new ProdutoController();
        produto = produtoControl.consulta(codigo);
    }

    private void vendeItem() {
        compoeItemParaVenda();
        vendaDetalhe = vendaControl.inserirItem(vendaDetalhe);
        listaVendaDetalhe.add(vendaDetalhe);
        imprimeItemBobina();
        Ecf.vendeItem(vendaDetalhe);
    }

    private void compoeItemParaVenda() {
        itemCupom++;
        vendaDetalhe.setIdProduto(produto.getId());
        vendaDetalhe.setIdCFOP(configuracao.getCFOPECF());
        vendaDetalhe.setIdTributo(produto.getIdTributo());
        vendaDetalhe.setIdVendaCabecalho(vendaCabecalho.getId());
        vendaDetalhe.setDescricaoPDV(produto.getDescricaoPDV());
        vendaDetalhe.setUnidadeProduto(produto.getUnidadeProduto());
        vendaDetalhe.setCST(produto.getSituacaoTributaria());
        vendaDetalhe.setECFICMS(produto.getECFICMS());
        vendaDetalhe.setTotalizadorParcial(produto.getTotalizadorParcial());
        vendaDetalhe.setGTIN(produto.getGTIN());
        vendaDetalhe.setItem(itemCupom);
        if (produto.getIPPT().equals("T")) {
            vendaDetalhe.setMovimentaEstoque("S");
        } else {
            vendaDetalhe.setMovimentaEstoque("N");
        }
        if (statusCaixa == 1) {
            vendaDetalhe.setQuantidade(Double.valueOf(editQuantidade.getText()));
            vendaDetalhe.setValorUnitario(produto.getValorVenda());
            vendaDetalhe.setValorTotal(Double.valueOf(editTotalItem.getText()));
        }
    }

    private void imprimeItemBobina() {
        String quantidade = formatter.format(vendaDetalhe.getQuantidade());
        String unitario = formatter.format(vendaDetalhe.getValorUnitario());
        String total = formatter.format(vendaDetalhe.getValorTotal());
        String descricao = vendaDetalhe.getDescricaoPDV();
        if (descricao.length() > 28) {
            descricao = descricao.substring(0, 28);
        }
        //linha 1 do item
        modelBobina.addElement(
                Biblioteca.repete("0", 3 - String.valueOf(itemCupom).length()) + itemCupom
                + "  "
                + vendaDetalhe.getGTIN() + Biblioteca.repete(" ", 14 - vendaDetalhe.getGTIN().length())
                + " "
                + descricao);

        //linha 2 do item
        modelBobina.addElement(
                Biblioteca.repete(" ", 8 - quantidade.length()) + quantidade
                + " "
                + vendaDetalhe.getUnidadeProduto().substring(0, 3)
                + " x "
                + Biblioteca.repete(" ", 13 - unitario.length()) + unitario
                + "  "
                + Biblioteca.repete(" ", 5 - vendaDetalhe.getECFICMS().length()) + vendaDetalhe.getECFICMS()
                + Biblioteca.repete(" ", 13 - total.length()) + total);
    }

    private void atualizaTotais() {
        String fSubTotal = formatter.format(subTotal);
        String fTotalGeral = formatter.format(totalGeral);
        editSubTotal.setText(fSubTotal);
        labelTotalGeral.setText(fTotalGeral);
        vendaCabecalho.setValorVenda(subTotal);
    }

    private void iniciaEncerramentoVenda() {
        if (statusCaixa == 1) {
            if (listaVendaDetalhe.size() > 0) {
                EfetuaPagamento telaEfetuaPagamento = new EfetuaPagamento(this, true);
                //telaEfetuaPagamento.setLocationRelativeTo(null);
                telaEfetuaPagamento.setVisible(true);
                if (telaEfetuaPagamento.pagamentoOK == true) {
                    concluiEncerramentoVenda();
                }
            } else {
                JOptionPane.showMessageDialog(rootPane, "Não existem itens para a venda..", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(rootPane, "Não existe venda em andamento.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void concluiEncerramentoVenda() {
        //TODO : precisamos fazer esses testes? porque?
        if (vendaCabecalho.getTotalProdutos() == null) {
            vendaCabecalho.setTotalProdutos(0.0);
        }
        if (vendaCabecalho.getTotalDocumento() == null) {
            vendaCabecalho.setTotalDocumento(0.0);
        }
        if (vendaCabecalho.getBaseICMS() == null) {
            vendaCabecalho.setBaseICMS(0.0);
        }
        if (vendaCabecalho.getTaxaDesconto() == null) {
            vendaCabecalho.setTaxaDesconto(0.0);
        }
        if (vendaCabecalho.getDesconto() == null) {
            vendaCabecalho.setDesconto(0.0);
        }
        if (vendaCabecalho.getTaxaAcrescimo() == null) {
            vendaCabecalho.setTaxaAcrescimo(0.0);
        }
        if (vendaCabecalho.getAcrescimo() == null) {
            vendaCabecalho.setAcrescimo(0.0);
        }
        if (vendaCabecalho.getTroco() == null) {
            vendaCabecalho.setTroco(0.0);
        }
        vendaControl.encerraVenda(vendaCabecalho);
        statusCaixa = 0;
        telaPadrao();
    }

    private void cancelaCupom() {
        if (statusCaixa == 1) {
            String[] opcoes = {"Sim", "Não"};
            int escolha = JOptionPane.showOptionDialog(null, "Deseja cancelar o cupom atual?", "Pergunta do Sistema",
                    JOptionPane.YES_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, opcoes, null);
            if (escolha == 0) {
                //TODO : É melhor persistir o valor nulo ou zerado? Por que?
                if (vendaCabecalho.getValorVenda() == null) {
                    vendaCabecalho.setValorVenda(0.0);
                }
                if (vendaCabecalho.getValorCancelado() == null) {
                    vendaCabecalho.setValorCancelado(0.0);
                }
                vendaControl.cancelaVenda(vendaCabecalho);
                Ecf.cancelaCupom();
                statusCaixa = 0;
                telaPadrao();
            }
        } else {
            JOptionPane.showMessageDialog(rootPane, "Não existe venda em andamento.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void cancelaItem() {
        if (statusCaixa == 1) {
            ValorInteiro janelaValorInteiro = new ValorInteiro(this, true);
            janelaValorInteiro.setLocationRelativeTo(null);
            janelaValorInteiro.setVisible(true);

            Integer cancela = janelaValorInteiro.retornaValor();

            if (cancela <= listaVendaDetalhe.size()) {
                vendaDetalhe = listaVendaDetalhe.get(cancela - 1);
                if (vendaDetalhe.getCancelado() == null) {
                    Ecf.cancelaItem(cancela);
                    vendaDetalhe.setCancelado("S");
                    vendaControl.cancelaItem(vendaDetalhe);

                    String descricao = vendaDetalhe.getDescricaoPDV();
                    if (descricao.length() > 28) {
                        descricao = descricao.substring(0, 28);
                    }

                    modelBobina.addElement(Biblioteca.repete("*", 48));
                    modelBobina.addElement(
                            Biblioteca.repete("0", 3 - String.valueOf(cancela).length()) + cancela
                            + "  "
                            + vendaDetalhe.getGTIN() + Biblioteca.repete(" ", 14 - vendaDetalhe.getGTIN().length())
                            + "  "
                            + descricao);
                    modelBobina.addElement("ITEM CANCELADO");
                    modelBobina.addElement(Biblioteca.repete("*", 48));
                    subTotal = subTotal - vendaDetalhe.getValorTotal();
                    totalGeral = totalGeral - vendaDetalhe.getValorTotal();
                    atualizaTotais();
                } else {
                    JOptionPane.showMessageDialog(rootPane, "O item solicitado já foi cancelado.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(rootPane, "O item solicitado não existe na venda atual.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(rootPane, "Não existe venda em andamento.", "Aviso do Sistema", JOptionPane.INFORMATION_MESSAGE);
        }
    }

//****************************************************************************//
// Aparência e controle dos painéis com as funções do programa - F1 a F12     //
//****************************************************************************//
    public void mouseClicked(MouseEvent e) {
        if (e.getSource() == panelF1) {
            identificaCliente();
        }
        if (e.getSource() == panelF2) {
            acionaMenuPrincipal();
        }
        if (e.getSource() == panelF3) {
            acionaMenuOperacoes();
        }
        if (e.getSource() == panelF4) {
            acionaMenuFiscal();
        }
        if (e.getSource() == panelF5) {
            //TODO : Devemos fazer uma calculadora própria?
            if (listaMenuPrincipal.getSelectedIndex() == 1) {
                try {
                    Runtime.getRuntime().exec("calc.exe");
                } catch (Exception ex) {
                }
            }
        }
        if (e.getSource() == panelF6) {
            localizaProduto();
        }
        if (e.getSource() == panelF7) {
            iniciaEncerramentoVenda();
        }
        if (e.getSource() == panelF8) {
            cancelaItem();
        }
        if (e.getSource() == panelF9) {
            cancelaCupom();
        }
        if (e.getSource() == panelF10) {
            descontoValor();
        }
        if (e.getSource() == panelF11) {
            identificaVendedor();
        }
        if (e.getSource() == panelF12) {
            sair();
        }
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
        if (e.getSource() == panelF1) {
            panelF1.setBackground(Color.WHITE);
        }
        if (e.getSource() == panelF2) {
            panelF2.setBackground(Color.WHITE);
        }
        if (e.getSource() == panelF3) {
            panelF3.setBackground(Color.WHITE);
        }
        if (e.getSource() == panelF4) {
            panelF4.setBackground(Color.WHITE);
        }
        if (e.getSource() == panelF5) {
            panelF5.setBackground(Color.WHITE);
        }
        if (e.getSource() == panelF6) {
            panelF6.setBackground(Color.WHITE);
        }
        if (e.getSource() == panelF7) {
            panelF7.setBackground(Color.WHITE);
        }
        if (e.getSource() == panelF8) {
            panelF8.setBackground(Color.WHITE);
        }
        if (e.getSource() == panelF9) {
            panelF9.setBackground(Color.WHITE);
        }
        if (e.getSource() == panelF10) {
            panelF10.setBackground(Color.WHITE);
        }
        if (e.getSource() == panelF11) {
            panelF11.setBackground(Color.WHITE);
        }
        if (e.getSource() == panelF12) {
            panelF12.setBackground(Color.WHITE);
        }
        this.repaint();
    }

    public void mouseExited(MouseEvent e) {
        if (e.getSource() == panelF1) {
            panelF1.setBackground(new Color(255, 255, 255, 0));
        }
        if (e.getSource() == panelF2) {
            panelF2.setBackground(new Color(255, 255, 255, 0));
        }
        if (e.getSource() == panelF3) {
            panelF3.setBackground(new Color(255, 255, 255, 0));
        }
        if (e.getSource() == panelF4) {
            panelF4.setBackground(new Color(255, 255, 255, 0));
        }
        if (e.getSource() == panelF5) {
            panelF5.setBackground(new Color(255, 255, 255, 0));
        }
        if (e.getSource() == panelF6) {
            panelF6.setBackground(new Color(255, 255, 255, 0));
        }
        if (e.getSource() == panelF7) {
            panelF7.setBackground(new Color(255, 255, 255, 0));
        }
        if (e.getSource() == panelF8) {
            panelF8.setBackground(new Color(255, 255, 255, 0));
        }
        if (e.getSource() == panelF9) {
            panelF9.setBackground(new Color(255, 255, 255, 0));
        }
        if (e.getSource() == panelF10) {
            panelF10.setBackground(new Color(255, 255, 255, 0));
        }
        if (e.getSource() == panelF11) {
            panelF11.setBackground(new Color(255, 255, 255, 0));
        }
        if (e.getSource() == panelF12) {
            panelF12.setBackground(new Color(255, 255, 255, 0));
        }
        this.repaint();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList bobina;
    private javax.swing.JLayeredPane containerPrincipal;
    private javax.swing.JFormattedTextField editCodigo;
    private javax.swing.JFormattedTextField editQuantidade;
    private javax.swing.JFormattedTextField editSubTotal;
    private javax.swing.JFormattedTextField editTotalItem;
    private javax.swing.JFormattedTextField editUnitario;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JLabel labelCaixa;
    private javax.swing.JLabel labelDescricaoProduto;
    private javax.swing.JLabel labelF1;
    private javax.swing.JLabel labelF10;
    private javax.swing.JLabel labelF11;
    private javax.swing.JLabel labelF12;
    private javax.swing.JLabel labelF2;
    private javax.swing.JLabel labelF3;
    private javax.swing.JLabel labelF4;
    private javax.swing.JLabel labelF5;
    private javax.swing.JLabel labelF6;
    private javax.swing.JLabel labelF7;
    private javax.swing.JLabel labelF8;
    private javax.swing.JLabel labelF9;
    private javax.swing.JLabel labelImagemProduto;
    private javax.swing.JLabel labelImagemTela;
    private javax.swing.JLabel labelMensagens;
    private javax.swing.JLabel labelOperador;
    private javax.swing.JLabel labelTitulo;
    private javax.swing.JLabel labelTotalGeral;
    private javax.swing.JList listaMenuFiscal;
    private javax.swing.JList listaMenuOperacoes;
    private javax.swing.JList listaMenuPrincipal;
    private javax.swing.JList listaSubMenuGerente;
    private javax.swing.JList listaSubMenuSupervisor;
    private javax.swing.JScrollPane panelBobina;
    private javax.swing.JPanel panelBotoes;
    private javax.swing.JPanel panelCard;
    private javax.swing.JPanel panelF1;
    private javax.swing.JPanel panelF10;
    private javax.swing.JPanel panelF11;
    private javax.swing.JPanel panelF12;
    private javax.swing.JPanel panelF2;
    private javax.swing.JPanel panelF3;
    private javax.swing.JPanel panelF4;
    private javax.swing.JPanel panelF5;
    private javax.swing.JPanel panelF6;
    private javax.swing.JPanel panelF7;
    private javax.swing.JPanel panelF8;
    private javax.swing.JPanel panelF9;
    private javax.swing.JPanel panelMenuFiscal;
    private javax.swing.JPanel panelMenuOperacoes;
    private javax.swing.JPanel panelMenuPrincipal;
    private javax.swing.JPanel panelSubMenu;
    private javax.swing.JScrollPane panelSubMenuGerente;
    private javax.swing.JScrollPane panelSubMenuSupervisor;
    // End of variables declaration//GEN-END:variables


    //TODO : Existe algum problema na implementação dessa Thread - Banner?
    class Banner implements Runnable {

        @Override
        public void run() {
            Random aleatorio = new Random();
            int numero = 0;
            try {
                while (true) {
                    if (statusCaixa == 0) {
                        numero = aleatorio.nextInt(6);
                        labelImagemProduto.setIcon(new javax.swing.ImageIcon(getClass().getResource(configuracao.getCaminhoImagensMarketing() + numero + ".jpg")));
                        Thread.sleep(5000);
                    }
                }
            } catch (Exception e) {
                labelImagemProduto.setIcon(new javax.swing.ImageIcon(getClass().getResource(configuracao.getCaminhoImagensProdutos() + "padrao.png")));
            }
        }
    }
}
