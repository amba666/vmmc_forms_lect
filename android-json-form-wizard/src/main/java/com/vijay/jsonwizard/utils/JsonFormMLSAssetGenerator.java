package com.vijay.jsonwizard.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

import static com.vijay.jsonwizard.constants.JsonFormConstants.KEY;
import static com.vijay.jsonwizard.constants.JsonFormConstants.MLS.PROPERTIES_FILE_NAME;
import static com.vijay.jsonwizard.constants.JsonFormConstants.STEP;
import static com.vijay.jsonwizard.constants.JsonFormConstants.TYPE;
import static com.vijay.jsonwizard.utils.Utils.getFileContentsAsString;

/**
 * Created by Vincent Karuri on 12/03/2020
 */
public class JsonFormMLSAssetGenerator {

    private static Map<String, String> placeholdersToTranslationsMap = new HashMap<>();
    private static String formName;

    private static final JsonFormInteractor jsonFormInteractor = JsonFormInteractor.getInstance();

    /**
     *
     * Processes the {@param formToTranslate} outputting a placeholder-injected form
     * and its corresponding property file
     *
     * @param formToTranslate
     */
    public static void processForm(String formToTranslate) {
        String form = getFileContentsAsString(formToTranslate);

        printToSystemOut("\nForm before placeholder injection:\n\n" + form);

        String[] formPath = formToTranslate.split(File.separator);
        formName = "placeholder_injected_" + formPath[formPath.length - 1].split("\\.")[0];

        JsonObject placeholderInjectedForm = injectPlaceholders(stringToJson(form), formName);
        placeholderInjectedForm.addProperty(PROPERTIES_FILE_NAME, formName);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String placeholderInjectedFormStr = gson.toJson(placeholderInjectedForm, JsonObject.class);
        writeToFile(placeholderInjectedFormStr, File.separator + "tmp" + File.separator + formName + ".json");

        printToSystemOut("\n\nPlaceholder-injected form: \n\n " + placeholderInjectedFormStr);

        createTranslationsPropertyFile();
    }

    /**
     *
     * Converts a {@link String} representation of JSON into a {@link JsonObject}
     *
     * @param json
     * @return
     */
    private static JsonObject stringToJson(String json) {
        return new Gson().fromJson(json, JsonObject.class);
    }

    /**
     *
     * Replaces {@link String} literals in the {@param jsonForm} with placeholders.
     *
     * The {@link String} literals (fields) to be replaced have to be defined either globally or as part of a widget's definition.
     *
     * The placeholders follow the scheme : {{form_name.step_name.widget_key.field_identifier}} for widget fields
     *
     * And {{form_name.step_name.field_identifier}} for step-level fields
     *
     * @param jsonForm
     * @param formName
     * @return
     */
    private static JsonObject injectPlaceholders(JsonObject jsonForm, String formName) {
        for (int i = 1; i <= getNumOfSteps(jsonForm); i++) {
            String stepName = STEP + i;
            String placeholderStringPrefix = formName + "." + stepName;
            replaceStepStringLiterals(placeholderStringPrefix, getStepJsonObject(jsonForm, stepName));
            replaceWidgetStringLiterals(placeholderStringPrefix, getWidgets(jsonForm, stepName));
        }
        return jsonForm;
    }

    /**
     *
     * Replaces {@link String} literals at step level with the appropriate placeholders
     *
     * @param stepJsonObject
     * @param placeholderStrPrefix
     */
    private static void replaceStepStringLiterals(String placeholderStrPrefix, JsonObject stepJsonObject) {
        for (String stepField : jsonFormInteractor.getDefaultTranslatableStepFields()) {
            JsonElement strLiteralElement = stepJsonObject.get(stepField);
            if (strLiteralElement != null) {
                String propertyName = placeholderStrPrefix + "." + stepField;
                String placeholderStr = "{{" + propertyName + "}}";
                placeholdersToTranslationsMap.put(propertyName, strLiteralElement.getAsString());
                stepJsonObject.addProperty(stepField, placeholderStr);
            }
        }
    }

    /**
     *
     * Replaces {@link String} literals in widgets with the appropriate placeholders
     *
     * @param rootPlaceholderPrefix
     * @param stepWidgets
     */
    private static void replaceWidgetStringLiterals(String rootPlaceholderPrefix, JsonArray stepWidgets) {
        for (int i = 0; i < stepWidgets.size(); i++) {
            JsonObject widget = stepWidgets.get(i).getAsJsonObject();
            String widgetKey = widget.get(KEY).getAsString();

            Set<String> translatableWidgetFields = jsonFormInteractor
                    .map
                    .get(widget.get(TYPE).getAsString())
                    .getCustomTranslatableWidgetFields();

            translatableWidgetFields.addAll(jsonFormInteractor.getDefaultTranslatableWidgetFields());

            for (String fieldIdentifier : translatableWidgetFields) {
                // Split the widget field identifier into it's constituent keys
                // and traverse the widget json object to get to the
                // base parent element(s) (the element(s) with a field to be replaced)
                String[] fieldIdentifierKeys = fieldIdentifier.split("\\.");
                JsonObject parentJsonObj = widget;
                JsonElement parentElement = widget;
                StringBuilder fieldIdentifierPrefix = new StringBuilder();
                for (int j = 0; j < fieldIdentifierKeys.length - 1; j++) {
                    String constituentFieldIdentifierKey = fieldIdentifierKeys[j];
                    parentElement = parentJsonObj.get(constituentFieldIdentifierKey);
                    if (parentElement != null) {
                        fieldIdentifierPrefix.append(constituentFieldIdentifierKey);
                        if (parentElement instanceof JsonArray) {
                            break; // support only one level of json array placeholder injection
                        } else {
                            parentJsonObj = parentElement.getAsJsonObject();
                        }
                    }
                }

                // package parent element(s) into json array
                if (parentElement != null) {
                    JsonArray parentElementsArr = new JsonArray();
                    parentElementsArr.add(parentElement);
                    parentElementsArr = parentElement instanceof JsonArray ? parentElement.getAsJsonArray() : parentElementsArr;

                    // if parent element is the widget itself, don't modify placeholder prefix
                    String widgetPlaceholderPrefix;
                    if (fieldIdentifierPrefix.toString().isEmpty()) {
                        widgetPlaceholderPrefix = rootPlaceholderPrefix;
                    } else {
                        widgetPlaceholderPrefix = rootPlaceholderPrefix + "." + widgetKey + "." + fieldIdentifierPrefix;
                    }

                    performReplacements(parentElementsArr, widgetPlaceholderPrefix, fieldIdentifierKeys[fieldIdentifierKeys.length - 1]);
                }
            }
        }
    }


    /**
     *
     * For each parent element in {@param parentElements}, replaces the {@param fieldToReplace}
     * with the appropriate placeholder derived from {@param placeholderPrefix}
     *
     * @param parentElements
     * @param placeholderPrefix
     * @param fieldToReplace
     */
    private static void performReplacements(JsonArray parentElements, String placeholderPrefix, String fieldToReplace){
        if (parentElements == null) {
            return;
        }

        for (int i = 0; i < parentElements.size(); i++) {
            String propertyName = placeholderPrefix;
            JsonObject parentElement = parentElements.get(i).getAsJsonObject();
            JsonElement parentElementKey = parentElement.get(KEY);

            // add unique key identifier if it exists
            if (parentElementKey != null) {
                propertyName += "." + parentElementKey.getAsString();
            }
            propertyName += "." + fieldToReplace;

            String placeholderStr = "{{" + propertyName + "}}";

            JsonElement fieldValueToReplace = parentElement.get(fieldToReplace);
            if (fieldValueToReplace != null) {
                placeholdersToTranslationsMap.put(propertyName, fieldValueToReplace.getAsString());
                parentElement.addProperty(fieldToReplace, placeholderStr);
            }
        }
    }

    /**
     *
     * Gets all the widget definitions for a particular {@param step} in the {@param jsonForm}
     *
     * @param jsonForm
     * @param step
     * @return
     */
    private static JsonArray getWidgets(JsonObject jsonForm, String step) {
        return getStepJsonObject(jsonForm, step).getAsJsonArray(JsonFormConstants.FIELDS);
    }

    /**
     *
     * Get the {@link JsonObject} representation of a {@param jsonForm} {@param step}
     *
     * @param jsonForm
     * @param step
     * @return
     */
    private static JsonObject getStepJsonObject(JsonObject jsonForm, String step) {
        return jsonForm.getAsJsonObject(step);
    }

    /**
     *
     * Extracts the number of steps in a {@param jsonForm}
     *
     * @param jsonForm
     * @return
     */
    private static int getNumOfSteps(JsonObject jsonForm) {
        return jsonForm.has(JsonFormConstants.COUNT) ? jsonForm.get(JsonFormConstants.COUNT).getAsInt() : -1;
    }

    /**
     *
     * Utility that prints to system out for debugging
     *
     * @param str
     */
    private static void printToSystemOut(String str) {
        System.out.println(str);
    }

    /**
     *
     * Creates a property file and writes it to disk
     *
     */
    private static void createTranslationsPropertyFile() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : placeholdersToTranslationsMap.entrySet()) {
            stringBuilder.append(entry.getKey() + " = " + entry.getValue().replace("\n", "\\n") + "\n"); // ensures \n is preserved in a String
        }
        writeToFile(stringBuilder.toString(), File.separator + "tmp" + File.separator + formName + ".properties");
    }

    /**
     *
     * Writes {@param data} to disk at the specified {@param path}
     *
     * @param data
     * @param path
     */
    private static void writeToFile(String data, String path) {
        try {
            Files.write(Paths.get(path), data.getBytes());
        } catch (IOException e) {
            Timber.e(e);
        }
    }

    public static void main(String[] args) {
        String formToTranslate = System.getenv("FORM_TO_TRANSLATE");
        printToSystemOut("Injecting placeholders in form at path: " + formToTranslate + " ...\n");
        processForm(formToTranslate);
    }
}