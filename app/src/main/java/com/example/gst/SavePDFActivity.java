package com.example.gst;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SavePDFActivity extends AppCompatActivity {

    @SuppressLint("StaticFieldLeak")
    private static Activity thisActivity;

    private static final int MAX_ITEMS_PER_PAGE = 32;
    private static Cursor pdfCursor;
    private static int totalItems;
    private static int itemsShown;
    private static PDFUtils pdfMaker;
    private static boolean moreItemsPresent;

    private static int totalPages;
    private static int pageNumber;

    private static String customerName;
    private static String billId;
    @SuppressLint("StaticFieldLeak")
    private static TextView totalQtyTv;
    @SuppressLint("StaticFieldLeak")
    private static TextView totalTaxableValueTv;
    @SuppressLint("StaticFieldLeak")
    private static TextView totalCgstTv;
    @SuppressLint("StaticFieldLeak")
    private static TextView totalSgstTv;
    @SuppressLint("StaticFieldLeak")
    private static TextView totalAmountBeforeTaxTv;
    @SuppressLint("StaticFieldLeak")
    private static TextView totalTaxAmountTv;
    @SuppressLint("StaticFieldLeak")
    private static TextView totalAmountAfterTaxTv;
    @SuppressLint("StaticFieldLeak")
    private static TextView totalAmountInWordsTv;
    @SuppressLint("StaticFieldLeak")
    private static TextView billPageNumber;
    @SuppressLint("StaticFieldLeak")
    private static SavePDFAdapter adapter;

    private static ContentResolver contentResolver;

    private static String inr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_pdf);

        thisActivity = this;

        Intent getPDFIntent = getIntent();
        customerName = getPDFIntent.getStringExtra(GSTBillingContract.GSTBillingEntry.PRIMARY_COLUMN_NAME);
        billId = getPDFIntent.getStringExtra(GSTBillingContract.GSTBillingEntry._ID);
        totalItems = getPDFIntent.getIntExtra(DetailActivity.ITEM_COUNT_KEY, 0);

        totalPages = (int) Math.ceil((double)totalItems/MAX_ITEMS_PER_PAGE);
        pageNumber = 0;

        TextView customerNameTv = (TextView) findViewById(R.id.pdf_customer_name);
        TextView invoiceDateTv = (TextView) findViewById(R.id.pdf_invoice_date);
        TextView businessNameTv = (TextView) findViewById(R.id.pdf_business_name);
        TextView businessAddressTv = (TextView) findViewById(R.id.pdf_business_address);
        TextView authorisedSignatoryTv = (TextView) findViewById(R.id.pdf_authorised_signatory);
        TextView businessContactTv = (TextView) findViewById(R.id.pdf_business_contact);
        totalQtyTv = (TextView) findViewById(R.id.pdf_total_qty);
        totalTaxableValueTv = (TextView) findViewById(R.id.pdf_total_taxable_value);
        totalCgstTv = (TextView) findViewById(R.id.pdf_total_cgst);
        totalSgstTv = (TextView) findViewById(R.id.pdf_total_sgst);
        totalAmountBeforeTaxTv = (TextView) findViewById(R.id.pdf_total_amount_before_tax);
        totalTaxAmountTv = (TextView) findViewById(R.id.pdf_total_gst);
        totalAmountAfterTaxTv = (TextView) findViewById(R.id.pdf_total_amount_after_tax);
        totalAmountInWordsTv = (TextView) findViewById(R.id.pdf_total_amount_in_words);
        billPageNumber = (TextView) findViewById(R.id.pdf_bill_page_number);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        businessNameTv.setText(prefs.getString(SetupPasswordActivity.SETUP_BUSINESS_NAME_KEY, getString(R.string.app_name)));
        businessAddressTv.setText(prefs.getString(SetupPasswordActivity.SETUP_BUSINESS_ADDRESS_KEY, "Address"));
        String authorisedSignatory = "For " + prefs.getString(SetupPasswordActivity.SETUP_BUSINESS_NAME_KEY, getString(R.string.app_name));
        authorisedSignatoryTv.setText(authorisedSignatory);
        businessContactTv.setText(prefs.getString(SetupPasswordActivity.SETUP_BUSINESS_CONTACT_KEY, ""));

        customerNameTv.setText(customerName);
        @SuppressLint("Recycle") Cursor billDateCursor = getContentResolver().query(
                GSTBillingContract.GSTBillingEntry.CONTENT_URI,
                new String[]{GSTBillingContract.GSTBillingEntry.PRIMARY_COLUMN_DATE},
                GSTBillingContract.GSTBillingEntry._ID + "=" + billId,
                null,
                null
        );
        billDateCursor.moveToFirst();
        String billDate = billDateCursor.getString(0);
        invoiceDateTv.setText(billDate);

        inr = getString(R.string.inr) + " ";

        contentResolver = getContentResolver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            pdfMaker = new PDFUtils();
        }

        itemsShown = 0;
        pdfCursor = updatePdfCursor();

        RecyclerView pdfRecyclerView = (RecyclerView) findViewById(R.id.pdf_recycler_view);
        pdfRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        pdfRecyclerView.setHasFixedSize(true);
        adapter = new SavePDFAdapter(this, pdfCursor);
        pdfRecyclerView.setAdapter(adapter);

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static void automaticSavePDF() {
        pdfMaker.addPageToPDF(thisActivity.findViewById(R.id.pdf_view));
        if(moreItemsPresent){
            pdfCursor = updatePdfCursor();
            adapter.swapCursor(pdfCursor);
        }else {
            pdfMaker.createPdf(thisActivity, billId + " " + customerName);
            thisActivity.finish();
        }
    }

    private static Cursor updatePdfCursor() {
        Cursor cursor = contentResolver.query(
                GSTBillingContract.GSTBillingEntry.CONTENT_URI.buildUpon().appendPath(billId).build(),
                null,
                GSTBillingContract.GSTBillingCustomerEntry._ID + " between " + (itemsShown + 1) + " and " + (itemsShown + MAX_ITEMS_PER_PAGE),
                null,
                GSTBillingContract.GSTBillingCustomerEntry._ID
        );
        itemsShown += MAX_ITEMS_PER_PAGE;
        moreItemsPresent = areMoreItemsPresent();
        setBillPageNumber();
        return cursor;
    }

    public static boolean areMoreItemsPresent(){
        return (totalItems - itemsShown) > 0;
    }

    @SuppressLint("SetTextI18n")
    private static void setBillPageNumber(){
        pageNumber++;
        billPageNumber.setText("Page " + pageNumber + " of " + totalPages);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_save_pdf, menu);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id==R.id.action_save_pdf_file){
                automaticSavePDF();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    public static void printTotalDetails(int totalQty, float totalTaxableValue, float totalSingleGst, float totalAmount){
        totalQtyTv.setText(String.valueOf(totalQty));
        totalTaxableValueTv.setText(String.format("%.2f", totalTaxableValue));
        totalCgstTv.setText(String.format("%.2f", totalSingleGst));
        totalSgstTv.setText(String.format("%.2f", totalSingleGst));
        totalAmountBeforeTaxTv.setText(inr + String.format("%.2f", totalTaxableValue));
        totalTaxAmountTv.setText(inr + String.format("%.2f", (totalSingleGst+totalSingleGst)));
        totalAmountAfterTaxTv.setText(inr + String.format("%.2f", totalAmount));
        totalAmountInWordsTv.setText(NumberToWord.getNumberInWords(String.valueOf((int)totalAmount)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            new Handler().postDelayed(SavePDFActivity::automaticSavePDF, 1000);
        }

    }
}