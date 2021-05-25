package ContabilityTemplateImportation;

import Dates.Dates;
import Entity.Executavel;
import JExcel.XLSX;
import TemplateContabil.Model.Entity.Importation;
import TemplateContabil.Model.Entity.LctoTemplate;
import TemplateContabil.Model.ImportationModel;
import fileManager.FileManager;
import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Model {

    public class converterArquivoParaTemplate extends Executavel {

        private final Importation importation;
        private final Integer mes;
        private final Integer ano;

        public converterArquivoParaTemplate(Importation importation, Integer mes, Integer ano) {
            this.importation = importation;
            this.mes = mes;
            this.ano = ano;
        }

        @Override
        public void run() {
            //Pega o arquivo PDF da importation
            File file = importation.getFile();

            //Cria Configuração
            Map<String, Map<String, String>> cfgCols = new HashMap<>();
            cfgCols.put("data", XLSX.convertStringToConfig("data", "-collumn¬A¬-type¬string¬-required¬true¬-regex¬(Titular|Imóvel) ?: ?[0-9]+|[0-3][0-9]\\/[0-1][0-9]\\/20[2-3][0-9]"));
            cfgCols.put("hist", XLSX.convertStringToConfig("hist", "-collumn¬B¬-type¬string"));
            cfgCols.put("debito", XLSX.convertStringToConfig("debito", "-collumn¬D¬-type¬value"));
            cfgCols.put("credito", XLSX.convertStringToConfig("credito", "-collumn¬E¬-type¬value"));

            //Pega os dados do Excel
            List<Map<String, Object>> rows = XLSX.get(file, cfgCols);

            String complemento = "";
            StringBuilder csvtext = new StringBuilder("#data;historico;debito;credito");

            //Percorre Excel
            for (Map<String, Object> row : rows) {
                //--Se tiver data na primeira coluna
                if (Dates.isDateInThisFormat(row.get("data").toString(), "dd/MM/yyyy")) {
                    csvtext.append("\r\n");
                    csvtext.append(row.get("data").toString()).append(";");
                    csvtext.append(row.get("hist").toString()).append(" ").append(complemento).append(";");

                    String debito = "";
                    String credito = "";

                    if (row.get("debito") != null) {
                        debito = ((BigDecimal) row.get("debito")).toPlainString();

                    } else if (row.get("credito") != null) {
                        credito = ((BigDecimal) row.get("credito")).toPlainString();
                    }

                    csvtext.append(debito).append(";");
                    csvtext.append(credito);

                    //Se tiver Titular ou Imóvel na primeira coluna    
                } else if (row.get("data").toString().startsWith("Titular") || row.get("data").toString().startsWith("Imóvel")) {
                    //Grava como Complemento de historico na hora que gravar cada um
                    complemento = row.get("data").toString();
                }
            }

            //Salva arquivo como CSV
            File newFile = new File(file.getParent() + "\\" + file.getName().replaceAll(".xlsx", ".csv"));
            FileManager.save(newFile, csvtext.toString());

            //Troca o arquivo file da importation
            importation.setFile(newFile);

            //Chama o modelo da importação que irá criar o template e gerar warning se algo der errado
            ImportationModel modelo = new ImportationModel(importation.getNome(), mes, ano, importation, null);

            //Pega lctos
            //List<LctoTemplate> lctos = importation.getLctos();
            modelo.criarTemplateDosLancamentos(importation);
        }
    }
}
