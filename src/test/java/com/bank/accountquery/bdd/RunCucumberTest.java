package com.bank.accountquery.bdd;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * BDD 進入點 — 透過 JUnit Platform Suite 啟動 Cucumber engine，
 * 掃描 classpath 下 features/ 內的 .feature 檔，glue 指向本套件的 Step Definitions。
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.bank.accountquery.bdd")
public class RunCucumberTest {
}
