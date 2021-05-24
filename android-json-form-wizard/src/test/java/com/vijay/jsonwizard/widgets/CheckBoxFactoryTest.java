package com.vijay.jsonwizard.widgets;

import android.content.res.Resources;
import android.support.constraint.ConstraintLayout;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.vijay.jsonwizard.BaseTest;
import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.activities.JsonFormActivity;
import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.interfaces.CommonListener;
import com.vijay.jsonwizard.utils.FormUtils;
import com.vijay.jsonwizard.views.CustomTextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class CheckBoxFactoryTest extends FactoryTest {
    private CheckBoxFactory factory;
    @Mock
    private JsonFormActivity context;
    @Mock
    private JsonFormFragment formFragment;
    @Mock
    private Resources resources;
    @Mock
    private CommonListener listener;
    @Mock
    private LinearLayout rootLayout;
    @Mock
    private DisplayMetrics displayMetrics;
    @Mock
    private ConstraintLayout constraintLayout;
    @Mock
    private ConstraintLayout constraintLayoutTwo;
    @Mock
    private ImageView imageView;
    @Mock
    private CustomTextView labelText;
    @Mock
    private ImageView editButton;
    @Mock
    private ConstraintLayout labelConstraintLayout;
    @Mock
    private LinearLayout checkboxLayout;
    @Mock
    private CheckBox checkBox;

    @Before
    public void setUp() {
        super.setUp();
        factory = new CheckBoxFactory();
    }

    @Test
    public void testCheckboxFactoryInstantiatesViewsCorrectly() throws Exception {
        String checkBoxWidgetString = "{\"key\":\"delivery_complications\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"161641AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"openmrs_data_type\":\"select one\",\"type\":\"check_box\",\"label\":\"Any delivery complications?\",\"label_text_size\":\"18sp\",\"text_size\":\"18sp\",\"label_text_color\":\"#FF9800\",\"hint\":\"Any delivery complications?\",\"read_only\":true,\"editable\":true,\"value\":\"['perineal_tear']\",\"exclusive\":[\"none\"],\"options\":[{\"key\":\"prolonged_obstructed_labour\",\"text\":\"Prolonged/obstructed labour\",\"value\":false,\"openmrs_choice_id\":\"160034AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"},{\"key\":\"abnormal_presentation\",\"text\":\"Abnormal presentation\",\"value\":false,\"openmrs_choice_id\":\"160034AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"text_color\":\"#FF9800\"},{\"key\":\"perineal_tear\",\"text\":\"Perineal tear (2, 3 or 4th degree)\",\"value\":false,\"openmrs_choice_id\":\"160034AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"},{\"key\":\"Other\",\"text\":\"Other\",\"value\":false,\"openmrs_choice_id\":\"160034AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"}],\"v_required\":{\"value\":true},\"relevance\":{\"rules-engine\":{\"ex-rules\":{\"rules-file\":\"sample-relevance-rules.yml\"}}},\"calculation\":{\"rules-engine\":{\"ex-rules\":{\"rules-file\":\"sample-calculations-rules.yml\"}}},\"constraints\":{\"rules-engine\":{\"ex-rules\":{\"rules-file\":\"sample-constraints-rules.yml\"}}}}";
        JSONObject checkBoxWidget = new JSONObject(checkBoxWidgetString);
        Assert.assertNotNull(factory);
        CheckBoxFactory factorySpy = Mockito.spy(factory);

        FormUtils formUtils = new FormUtils();
        FormUtils formUtilsSpy = Mockito.spy(formUtils);
        Whitebox.setInternalState(factorySpy, "formUtils", formUtilsSpy);

        Mockito.doReturn(resources).when(context).getResources();
        Assert.assertNotNull(resources);

        jsonFormActivity.setTheme(R.style.NativeFormsAppTheme);
//        rootLayout.setTag(R.id.address, "RandomStepName:delivery_complications");
        Mockito.doReturn(rootLayout).when(factorySpy).getLinearLayout(context);
        Mockito.doReturn("RandomStepName:delivery_complications").when(rootLayout).getTag(R.id.address);
        Mockito.doReturn(checkboxLayout).when(factorySpy).getCheckboxLayout(context);
        Mockito.doReturn(displayMetrics).when(resources).getDisplayMetrics();
        Mockito.doReturn(constraintLayout).when(formUtilsSpy).getRootConstraintLayout(context);
        Mockito.doReturn(checkBox).when(checkboxLayout).findViewById(R.id.checkbox);
        Mockito.doReturn(imageView).when(checkboxLayout).findViewById(R.id.checkbox_info_icon);
        Mockito.doReturn(constraintLayoutTwo).when(formUtilsSpy).getConstraintLayout(ArgumentMatchers.anyString(), ArgumentMatchers.eq(new JSONArray()), ArgumentMatchers.eq(checkBoxWidget), ArgumentMatchers.eq(context), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        Mockito.doNothing().when(formUtilsSpy).showInfoIcon(ArgumentMatchers.anyString(), ArgumentMatchers.eq(new JSONObject()), ArgumentMatchers.eq(listener), ArgumentMatchers.eq(new HashMap<String, String>()), ArgumentMatchers.eq(imageView), ArgumentMatchers.eq(new JSONArray()));
        Mockito.doReturn(labelText).when(labelConstraintLayout).findViewById(R.id.label_text);
        Mockito.doReturn(editButton).when(labelConstraintLayout).findViewById(R.id.label_edit_button);
        Mockito.doReturn(labelConstraintLayout).when(formUtilsSpy).createLabelLinearLayout(ArgumentMatchers.anyString(), ArgumentMatchers.eq(new JSONArray()), ArgumentMatchers.eq(new JSONObject()), ArgumentMatchers.eq(context), ArgumentMatchers.eq(listener));

        //noinspection ResultOfMethodCallIgnored
        Mockito.doReturn(jsonFormActivity).when(formFragment).getJsonApi();

        List<View> viewList = factorySpy.getViewsFromJson("RandomStepName", context, formFragment, checkBoxWidget, listener);
        Assert.assertNotNull(viewList);
        Assert.assertEquals(1, viewList.size());
    }

    @Test
    public void testGetCustomTranslatableWidgetFields() {
        Assert.assertNotNull(factory);
        CheckBoxFactory factorySpy = Mockito.spy(factory);

        Set<String> editableProperties = factorySpy.getCustomTranslatableWidgetFields();
        Assert.assertEquals(1, editableProperties.size());
        Assert.assertEquals("options.text", editableProperties.iterator().next());
    }
}
