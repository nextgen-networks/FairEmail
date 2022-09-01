package eu.faircode.email;

/*
    This file is part of FairEmail.

    FairEmail is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FairEmail is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FairEmail.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2018-2022 by Marcel Bokhorst (M66B)
*/

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_FIRST_USER;
import static android.app.Activity.RESULT_OK;
import static android.system.OsConstants.ENOSPC;
import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN;
import static android.widget.AdapterView.INVALID_POSITION;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.OperationCanceledException;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.security.KeyChain;
import android.system.ErrnoException;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.CharacterStyle;
import android.text.style.ImageSpan;
import android.text.style.ParagraphStyle;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuggestionSpan;
import android.text.style.URLSpan;
import android.util.LogPrinter;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.documentfile.provider.DocumentFile;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;
import com.google.android.material.snackbar.Snackbar;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSProcessableFile;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.RecipientInfoGenerator;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyAgreeRecipientInfoGenerator;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeFilter;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;
import org.w3c.dom.css.CSSStyleSheet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.Collator;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessageRemovedException;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.ParseException;
import javax.mail.util.ByteArrayDataSource;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.property.Organizer;

public class FragmentCompose extends FragmentBase {
    private enum State {NONE, LOADING, LOADED}

    private ViewGroup view;
    private View vwAnchorMenu;
    private Spinner spIdentity;
    private EditText etExtra;
    private TextView tvDomain;
    private MultiAutoCompleteTextView etTo;
    private ImageButton ibToAdd;
    private MultiAutoCompleteTextView etCc;
    private ImageButton ibCcAdd;
    private MultiAutoCompleteTextView etBcc;
    private ImageButton ibBccAdd;
    private EditText etSubject;
    private ImageButton ibCcBcc;
    private ImageButton ibRemoveAttachments;
    private RecyclerView rvAttachment;
    private TextView tvNoInternetAttachments;
    private TextView tvDsn;
    private TextView tvResend;
    private TextView tvPlainTextOnly;
    private EditTextCompose etBody;
    private TextView tvNoInternet;
    private TextView tvSignature;
    private CheckBox cbSignature;
    private ImageButton ibSignature;
    private TextView tvReference;
    private ImageButton ibCloseRefHint;
    private ImageButton ibWriteAboveBelow;
    private TextView tvLanguage;
    private ImageButton ibReferenceEdit;
    private ImageButton ibReferenceImages;
    private View vwAnchor;
    private TextViewAutoCompleteAction etSearch;
    private BottomNavigationView style_bar;
    private BottomNavigationView media_bar;
    private BottomNavigationView bottom_navigation;
    private ContentLoadingProgressBar pbWait;
    private Group grpHeader;
    private Group grpExtra;
    private Group grpAddresses;
    private Group grpAttachments;
    private Group grpBody;
    private Group grpSignature;
    private Group grpReferenceHint;

    private ContentResolver resolver;
    private AdapterAttachment adapter;

    private String compose_font;
    private String display_font;
    private boolean dsn = true;
    private Integer encrypt = null;
    private boolean media = true;
    private boolean compact = false;
    private int zoom = 0;

    private long working = -1;
    private State state = State.NONE;
    private boolean identity_selectable = false;
    private boolean show_images = false;
    private Integer last_plain_only = null;
    private List<EntityAttachment> last_attachments = null;
    private boolean saved = false;
    private String subject = null;

    private Uri photoURI = null;

    private int pickRequest;
    private Uri pickUri;

    private String[] pgpUserIds;
    private long[] pgpKeyIds;
    private long pgpSignKeyId;

    private int searchIndex = 0;

    private static final int REDUCED_IMAGE_SIZE = 1440; // pixels
    private static final int REDUCED_IMAGE_QUALITY = 90; // percent
    // http://regex.info/blog/lightroom-goodies/jpeg-quality

    private static final int MAX_SHOW_RECIPIENTS = 5;
    private static final int RECIPIENTS_WARNING = 10;

    private static final int MAX_QUOTE_LEVEL = 5;

    private static final int REQUEST_CONTACT_TO = 1;
    private static final int REQUEST_CONTACT_CC = 2;
    private static final int REQUEST_CONTACT_BCC = 3;
    private static final int REQUEST_SHARED = 4;
    private static final int REQUEST_IMAGE = 5;
    private static final int REQUEST_IMAGE_FILE = 6;
    private static final int REQUEST_ATTACHMENT = 7;
    private static final int REQUEST_TAKE_PHOTO = 8;
    private static final int REQUEST_RECORD_AUDIO = 9;
    private static final int REQUEST_OPENPGP = 10;
    private static final int REQUEST_CONTACT_GROUP = 11;
    private static final int REQUEST_LINK = 12;
    private static final int REQUEST_DISCARD = 13;
    private static final int REQUEST_SEND = 14;
    private static final int REQUEST_REMOVE_ATTACHMENTS = 15;

    private static ExecutorService executor = Helper.getBackgroundExecutor(1, "compose");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        compose_font = prefs.getString("compose_font", "");
        display_font = prefs.getString("display_font", "");
        media = prefs.getBoolean("compose_media", true);
        compact = prefs.getBoolean("compose_compact", false);
        zoom = prefs.getInt("compose_zoom", compact ? 0 : 1);

        setTitle(R.string.page_compose);
        setSubtitle(getResources().getQuantityString(R.plurals.page_message, 1));
    }

    @Override
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = (ViewGroup) inflater.inflate(R.layout.fragment_compose, container, false);

        // Get controls
        vwAnchorMenu = view.findViewById(R.id.vwAnchorMenu);
        spIdentity = view.findViewById(R.id.spIdentity);
        etExtra = view.findViewById(R.id.etExtra);
        tvDomain = view.findViewById(R.id.tvDomain);
        etTo = view.findViewById(R.id.etTo);
        ibToAdd = view.findViewById(R.id.ibToAdd);
        etCc = view.findViewById(R.id.etCc);
        ibCcAdd = view.findViewById(R.id.ibCcAdd);
        etBcc = view.findViewById(R.id.etBcc);
        ibBccAdd = view.findViewById(R.id.ibBccAdd);
        etSubject = view.findViewById(R.id.etSubject);
        ibCcBcc = view.findViewById(R.id.ibCcBcc);
        ibRemoveAttachments = view.findViewById(R.id.ibRemoveAttachments);
        rvAttachment = view.findViewById(R.id.rvAttachment);
        tvNoInternetAttachments = view.findViewById(R.id.tvNoInternetAttachments);
        tvDsn = view.findViewById(R.id.tvDsn);
        tvResend = view.findViewById(R.id.tvResend);
        tvPlainTextOnly = view.findViewById(R.id.tvPlainTextOnly);
        etBody = view.findViewById(R.id.etBody);
        tvNoInternet = view.findViewById(R.id.tvNoInternet);
        tvSignature = view.findViewById(R.id.tvSignature);
        cbSignature = view.findViewById(R.id.cbSignature);
        ibSignature = view.findViewById(R.id.ibSignature);
        tvReference = view.findViewById(R.id.tvReference);
        ibCloseRefHint = view.findViewById(R.id.ibCloseRefHint);
        ibWriteAboveBelow = view.findViewById(R.id.ibWriteAboveBelow);
        tvLanguage = view.findViewById(R.id.tvLanguage);
        ibReferenceEdit = view.findViewById(R.id.ibReferenceEdit);
        ibReferenceImages = view.findViewById(R.id.ibReferenceImages);
        vwAnchor = view.findViewById(R.id.vwAnchor);
        etSearch = view.findViewById(R.id.etSearch);
        style_bar = view.findViewById(R.id.style_bar);
        media_bar = view.findViewById(R.id.media_bar);
        bottom_navigation = view.findViewById(R.id.bottom_navigation);

        pbWait = view.findViewById(R.id.pbWait);
        grpHeader = view.findViewById(R.id.grpHeader);
        grpExtra = view.findViewById(R.id.grpExtra);
        grpAddresses = view.findViewById(R.id.grpAddresses);
        grpAttachments = view.findViewById(R.id.grpAttachments);
        grpBody = view.findViewById(R.id.grpBody);
        grpSignature = view.findViewById(R.id.grpSignature);
        grpReferenceHint = view.findViewById(R.id.grpReferenceHint);

        resolver = getContext().getContentResolver();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        final boolean auto_save_paragraph = prefs.getBoolean("auto_save_paragraph", true);
        final boolean auto_save_dot = prefs.getBoolean("auto_save_dot", false);
        final boolean keyboard_no_fullscreen = prefs.getBoolean("keyboard_no_fullscreen", false);
        final boolean suggest_names = prefs.getBoolean("suggest_names", true);
        final boolean suggest_sent = prefs.getBoolean("suggest_sent", true);
        final boolean suggest_received = prefs.getBoolean("suggest_received", false);
        final boolean suggest_frequently = prefs.getBoolean("suggest_frequently", false);
        final boolean cc_bcc = prefs.getBoolean("cc_bcc", false);
        final boolean circular = prefs.getBoolean("circular", true);

        final float dp3 = Helper.dp2pixels(getContext(), 3);

        // Wire controls
        spIdentity.setOnItemSelectedListener(identitySelected);

        View.OnTouchListener onTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    EditText et = (EditText) v;
                    int start = et.getSelectionStart();
                    int end = et.getSelectionEnd();

                    if (start < 0 || end < 0)
                        return false;

                    if (start == end)
                        return false;

                    if (start > end) {
                        int tmp = start;
                        start = end;
                        end = tmp;
                    }

                    float x = event.getX() + et.getScrollX();
                    float y = event.getY() + et.getScrollY();
                    int pos = et.getOffsetForPosition(x, y);
                    if (pos < 0)
                        return false;

                    // Undo selection to be able to select another address
                    if (pos < start || pos >= end)
                        et.setSelection(pos);
                }

                return false;
            }
        };

        View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                EditText et = (EditText) v;
                int start = et.getSelectionStart();
                int end = et.getSelectionEnd();

                if (start < 0 || end < 0)
                    return false;

                if (start > end) {
                    int tmp = start;
                    start = end;
                    end = tmp;
                }

                String text = et.getText().toString();
                if (text.length() == 0)
                    return false;

                int last = text.indexOf(',', start);
                last = (last < 0 ? text.length() - 1 : last);

                int first = text.substring(0, last).lastIndexOf(',');
                first = (first < 0 ? 0 : first + 1);

                et.setSelection(first, last + 1);

                return false;
            }
        };

        etTo.setMaxLines(Integer.MAX_VALUE);
        etTo.setHorizontallyScrolling(false);
        etTo.setOnTouchListener(onTouchListener);
        etTo.setOnLongClickListener(longClickListener);

        etCc.setMaxLines(Integer.MAX_VALUE);
        etCc.setHorizontallyScrolling(false);
        etCc.setOnTouchListener(onTouchListener);
        etCc.setOnLongClickListener(longClickListener);

        etBcc.setMaxLines(Integer.MAX_VALUE);
        etBcc.setHorizontallyScrolling(false);
        etBcc.setOnTouchListener(onTouchListener);
        etBcc.setOnLongClickListener(longClickListener);

        etSubject.setMaxLines(Integer.MAX_VALUE);
        etSubject.setHorizontallyScrolling(false);

        ibCcBcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMenuAddresses();
            }
        });

        ibCcBcc.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onMenuAddresses();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                prefs.edit().putBoolean("cc_bcc", grpAddresses.getVisibility() == View.VISIBLE).apply();
                ToastEx.makeText(v.getContext(), R.string.title_default_changed, Toast.LENGTH_LONG).show();
                return true;
            }
        });

        View.OnClickListener onPick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int request;
                int id = view.getId();
                if (id == R.id.ibToAdd) {
                    request = REQUEST_CONTACT_TO;
                } else if (id == R.id.ibCcAdd) {
                    request = REQUEST_CONTACT_CC;
                } else if (id == R.id.ibBccAdd) {
                    request = REQUEST_CONTACT_BCC;
                } else {
                    return;
                }

                // https://developer.android.com/guide/topics/providers/contacts-provider#Intents
                Intent pick = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Email.CONTENT_URI);
                pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(Helper.getChooser(getContext(), pick), request);
            }
        };

        ibToAdd.setOnClickListener(onPick);
        ibCcAdd.setOnClickListener(onPick);
        ibBccAdd.setOnClickListener(onPick);

        View.OnLongClickListener onGroup = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                int id = view.getId();
                if (id == R.id.ibToAdd) {
                    onMenuContactGroup(etTo);
                    return true;
                } else if (id == R.id.ibCcAdd) {
                    onMenuContactGroup(etCc);
                    return true;
                } else if (id == R.id.ibBccAdd) {
                    onMenuContactGroup(etBcc);
                    return true;
                } else
                    return true;
            }
        };

        ibToAdd.setOnLongClickListener(onGroup);
        ibCcAdd.setOnLongClickListener(onGroup);
        ibBccAdd.setOnLongClickListener(onGroup);

        tvPlainTextOnly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putBoolean("force_dialog", true);
                onAction(R.id.action_check, args, "force");
            }
        });

        setZoom();

        etBody.setInputContentListener(new EditTextCompose.IInputContentListener() {
            @Override
            public void onInputContent(Uri uri) {
                Log.i("Received input uri=" + uri);
                onAddAttachment(Arrays.asList(uri), true, 0, false);
            }
        });

        etBody.setSelectionListener(new EditTextCompose.ISelection() {
            private boolean styling = false;

            @Override
            public void onSelected(final boolean selection) {
                if (media) {
                    getMainHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                                return;
                            if (styling != selection) {
                                styling = selection;
                                media_bar.getMenu().clear();
                                media_bar.inflateMenu(styling
                                        ? R.menu.action_compose_style_alt
                                        : R.menu.action_compose_media);
                                invalidateOptionsMenu();
                            }
                        }
                    }, 20);
                } else
                    style_bar.setVisibility(selection ? View.VISIBLE : View.GONE);
            }
        });

        // https://developer.android.com/reference/android/text/TextWatcher
        etBody.addTextChangedListener(new TextWatcher() {
            private boolean save = false;
            private Integer added = null;
            private Integer removed = null;
            private Integer inserted = null;

            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
                if (count == 1 && after == 0 && (start == 0 || text.charAt(start) == '\n')) {
                    Log.i("Removed=" + start);
                    removed = start;
                }

                if (BuildConfig.DEBUG && count - after == 1 && start + after > 0) {
                    int replaced = start + after;
                    Spanned spanned = ((Spanned) text);
                    StyleHelper.InsertedSpan[] spans =
                            spanned.getSpans(replaced, replaced, StyleHelper.InsertedSpan.class);
                    for (StyleHelper.InsertedSpan span : spans) {
                        int end = spanned.getSpanEnd(span);
                        Log.i("Replaced=" + replaced);
                        if (end - 1 == replaced) {
                            inserted = end - 1;
                            break;
                        }
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                Activity activity = getActivity();
                if (activity != null)
                    activity.onUserInteraction();

                int index = start + before;

                if (count - before == 1 && index > 0) {
                    char c = text.charAt(index);
                    char b = text.charAt(index - 1);
                    save = (auto_save_paragraph && c == '\n' && b != '\n') ||
                            (auto_save_dot && Helper.isDot(c) && !Helper.isDot(b));
                    if (save)
                        Log.i("Save=" + index);

                    if (c == '\n') {
                        Log.i("Added=" + index);
                        added = index;
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable text) {
                if (etBody == null)
                    return;

                LogPrinter lp = null;
                if (BuildConfig.DEBUG &&
                        (added != null || removed != null))
                    lp = new LogPrinter(android.util.Log.INFO, "FairEmail");

                if (lp != null)
                    TextUtils.dumpSpans(text, new LogPrinter(android.util.Log.INFO, "FairEmail"), "---before>");

                if (added != null)
                    try {
                        // break block quotes
                        boolean broken = false;
                        QuoteSpan[] spans = text.getSpans(added + 1, added + 1, QuoteSpan.class);
                        for (QuoteSpan span : spans) {
                            int s = text.getSpanStart(span);
                            int e = text.getSpanEnd(span);
                            int f = text.getSpanFlags(span);
                            Log.i(span + " " + s + "..." + e + " added=" + added);

                            if (s > 0 && added - s > 0 && e - (added + 1) > 0 &&
                                    text.charAt(s - 1) == '\n' && text.charAt(added - 1) == '\n' &&
                                    text.charAt(added) == '\n' && text.charAt(e - 1) == '\n') {
                                broken = true;

                                QuoteSpan q1 = StyleHelper.clone(span, QuoteSpan.class, etBody.getContext());
                                text.setSpan(q1, s, added, f);
                                Log.i(span + " " + s + "..." + added);

                                QuoteSpan q2 = StyleHelper.clone(span, QuoteSpan.class, etBody.getContext());
                                text.setSpan(q2, added + 1, e, f);
                                Log.i(span + " " + (added + 1) + "..." + e);

                                text.removeSpan(span);
                            }
                        }

                        if (broken) {
                            CharacterStyle[] sspan = text.getSpans(added + 1, added + 1, CharacterStyle.class);
                            for (CharacterStyle span : sspan) {
                                int s = text.getSpanStart(span);
                                int e = text.getSpanEnd(span);
                                int f = text.getSpanFlags(span);
                                Log.i(span + " " + s + "..." + e + " start=" + added);

                                if (s <= added && added + 1 <= e) {
                                    CharacterStyle s1 = CharacterStyle.wrap(span);
                                    text.setSpan(s1, s, added, f);
                                    Log.i(span + " " + s + "..." + added);

                                    CharacterStyle s2 = CharacterStyle.wrap(span);
                                    text.setSpan(s2, added + 1, e, f);
                                    Log.i(span + " " + (added + 1) + "..." + e);

                                    text.removeSpan(span);
                                }
                            }

                            etBody.setSelection(added);
                        }

                        // Escape indent at end
                        IndentSpan[] indents = text.getSpans(added + 1, added + 1, IndentSpan.class);
                        for (IndentSpan indent : indents) {
                            int s = text.getSpanStart(indent);
                            int e = text.getSpanEnd(indent);
                            int f = text.getSpanFlags(indent);
                            if (e - 1 > s && added + 1 == e) {
                                text.removeSpan(indent);
                                text.setSpan(new IndentSpan(indent.getLeadingMargin(true)), s, e - 1, f);
                            }
                        }

                        boolean renum = false;
                        BulletSpan[] bullets = text.getSpans(added + 1, added + 1, BulletSpan.class);

                        int len = 0;
                        BulletSpan shortest = null;
                        for (BulletSpan span : bullets) {
                            int s = text.getSpanStart(span);
                            int e = text.getSpanEnd(span);
                            if (shortest == null || e - s < len) {
                                shortest = span;
                                len = e - s;
                            }
                        }

                        if (shortest != null) {
                            int s = text.getSpanStart(shortest);
                            int e = text.getSpanEnd(shortest);
                            int f = text.getSpanFlags(shortest) | Spanned.SPAN_PARAGRAPH;
                            Log.i(shortest + " " + s + "..." + e + " added=" + added);

                            if (s > 0 &&
                                    added + 1 > s && e > added + 1 &&
                                    text.charAt(s - 1) == '\n' && text.charAt(e - 1) == '\n') {
                                if (e - s > 2) {
                                    BulletSpan b1 = StyleHelper.clone(shortest, shortest.getClass(), etBody.getContext());
                                    text.setSpan(b1, s, added + 1, f);
                                    Log.i(shortest + " " + s + "..." + (added + 1));

                                    BulletSpan b2 = StyleHelper.clone(b1, shortest.getClass(), etBody.getContext());
                                    text.setSpan(b2, added + 1, e, f);
                                    Log.i(shortest + " " + (added + 1) + "..." + e);
                                }

                                renum = true;
                                text.removeSpan(shortest);
                            }
                        }

                        if (renum)
                            StyleHelper.renumber(text, false, etBody.getContext());
                    } catch (Throwable ex) {
                        Log.e(ex);
                    } finally {
                        added = null;
                    }

                if (removed != null)
                    try {
                        ParagraphStyle[] ps = text.getSpans(removed, removed + 1, ParagraphStyle.class);
                        if (ps != null)
                            for (ParagraphStyle p : ps) {
                                int start = text.getSpanStart(p);
                                int end = text.getSpanEnd(p);
                                if (start >= removed && end <= removed + 1)
                                    text.removeSpan(p);
                            }

                        StyleHelper.renumber(text, true, etBody.getContext());
                    } finally {
                        removed = null;
                    }

                if (inserted != null)
                    try {
                        StyleHelper.InsertedSpan[] spans =
                                text.getSpans(inserted, inserted, StyleHelper.InsertedSpan.class);
                        for (StyleHelper.InsertedSpan span : spans) {
                            int start = text.getSpanStart(span);
                            int end = text.getSpanEnd(span);
                            if (end == inserted) {
                                text.delete(start, end);
                                text.removeSpan(span);
                            }
                        }
                    } finally {
                        inserted = null;
                    }

                if (save)
                    try {
                        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                            Bundle extras = new Bundle();
                            extras.putBoolean("silent", true);
                            onAction(R.id.action_save, extras, "paragraph");
                        }
                    } finally {
                        save = false;
                    }

                if (lp != null)
                    TextUtils.dumpSpans(text, lp, "---after>");
            }
        });

        tvSignature.setTypeface(StyleHelper.getTypeface(compose_font, getContext()));

        cbSignature.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                Object tag = cbSignature.getTag();
                if (tag == null || !tag.equals(checked)) {
                    cbSignature.setTag(checked);
                    tvSignature.setAlpha(checked ? 1.0f : Helper.LOW_LIGHT);
                    if (tag != null)
                        onAction(R.id.action_save, "signature");
                }
            }
        });

        ibSignature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EntityIdentity identity = (EntityIdentity) spIdentity.getSelectedItem();
                if (identity == null || TextUtils.isEmpty(identity.signature))
                    return;

                ClipboardManager clipboard = Helper.getSystemService(v.getContext(), ClipboardManager.class);
                if (clipboard == null)
                    return;

                ClipData clip = ClipData.newHtmlText(
                        v.getContext().getString(R.string.title_edit_signature_text),
                        HtmlHelper.getText(v.getContext(), identity.signature),
                        identity.signature);
                clipboard.setPrimaryClip(clip);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                    ToastEx.makeText(v.getContext(), R.string.title_clipboard_copied, Toast.LENGTH_LONG).show();
            }
        });

        ibCloseRefHint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(v.getContext());
                prefs.edit().putBoolean("compose_reference", false).apply();
                grpReferenceHint.setVisibility(View.GONE);
            }
        });

        ibWriteAboveBelow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(v.getContext());
                boolean write_below = !prefs.getBoolean("write_below", false);
                prefs.edit().putBoolean("write_below", write_below).apply();
                ibWriteAboveBelow.setImageLevel(write_below ? 1 : 0);
                ToastEx.makeText(v.getContext(),
                        write_below
                                ? R.string.title_advanced_write_below
                                : R.string.title_advanced_write_above,
                        Toast.LENGTH_LONG).show();
            }
        });

        ibReferenceEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onReferenceEdit();
            }
        });

        ibReferenceImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(v.getContext())
                        .setMessage(R.string.title_ask_show_image)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ibReferenceImages.setVisibility(View.GONE);
                                onReferenceImages();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });

        etBody.setTypeface(StyleHelper.getTypeface(compose_font, getContext()));
        tvReference.setTypeface(StyleHelper.getTypeface(display_font, getContext()));

        tvReference.setMovementMethod(new ArrowKeyMovementMethod() {
            @Override
            public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    int off = Helper.getOffset(widget, buffer, event);
                    URLSpan[] link = buffer.getSpans(off, off, URLSpan.class);
                    if (link.length > 0) {
                        String url = link[0].getURL();
                        Uri uri = Uri.parse(url);
                        if (uri.getScheme() == null)
                            uri = Uri.parse("https://" + url);

                        int start = buffer.getSpanStart(link[0]);
                        int end = buffer.getSpanEnd(link[0]);
                        String title = (start < 0 || end < 0 || end <= start
                                ? null : buffer.subSequence(start, end).toString());
                        if (url.equals(title))
                            title = null;

                        Bundle args = new Bundle();
                        args.putParcelable("uri", uri);
                        args.putString("title", title);

                        FragmentDialogOpenLink fragment = new FragmentDialogOpenLink();
                        fragment.setArguments(args);
                        fragment.show(getParentFragmentManager(), "open:link");

                        return true;
                    }
                }

                return super.onTouchEvent(widget, buffer, event);
            }
        });

        etSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus)
                    endSearch();
            }
        });

        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    endSearch();
                    return true;
                } else
                    return false;
            }
        });

        etSearch.setActionRunnable(new Runnable() {
            @Override
            public void run() {
                performSearch(true);
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(false);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Do nothing
            }
        });

        style_bar.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int action = item.getItemId();
                return onActionStyle(action, style_bar.findViewById(action));
            }
        });

        media_bar.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int action = item.getItemId();
                if (action == R.id.menu_record_audio) {
                    onActionRecordAudio();
                    return true;
                } else if (action == R.id.menu_take_photo) {
                    onActionImage(true, false);
                    return true;
                } else if (action == R.id.menu_image) {
                    onActionImage(false, false);
                    return true;
                } else if (action == R.id.menu_attachment) {
                    onActionAttachment();
                    return true;
                } else if (action == R.id.menu_link) {
                    onActionLink();
                    return true;
                }
                return onActionStyle(action, media_bar.findViewById(action));
            }
        });

        setCompact(compact);

        bottom_navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                final int action = item.getItemId();
                if (action == R.id.action_delete) {
                    onActionDiscard();
                } else if (action == R.id.action_send) {
                    onAction(R.id.action_check, "check");
                } else if (action == R.id.action_save) {
                    saved = true;
                    onAction(action, "save");
                } else {
                    onAction(action, "navigation");
                }
                return true;
            }
        });

        addKeyPressedListener(onKeyPressedListener);
        setBackPressedCallback(backPressedCallback);

        // Initialize
        setHasOptionsMenu(true);
        FragmentDialogTheme.setBackground(getContext(), view, true);

        if (keyboard_no_fullscreen) {
            // https://developer.android.com/reference/android/view/inputmethod/EditorInfo#IME_FLAG_NO_FULLSCREEN
            etExtra.setImeOptions(etExtra.getImeOptions() | IME_FLAG_NO_FULLSCREEN);
            etTo.setImeOptions(etTo.getImeOptions() | IME_FLAG_NO_FULLSCREEN);
            etCc.setImeOptions(etCc.getImeOptions() | IME_FLAG_NO_FULLSCREEN);
            etBcc.setImeOptions(etBcc.getImeOptions() | IME_FLAG_NO_FULLSCREEN);
            etSubject.setImeOptions(etSubject.getImeOptions() | IME_FLAG_NO_FULLSCREEN);
            etBody.setImeOptions(etBody.getImeOptions() | IME_FLAG_NO_FULLSCREEN);
        }

        etExtra.setHint("");
        tvDomain.setText(null);
        tvDsn.setVisibility(View.GONE);
        tvResend.setVisibility(View.GONE);
        tvPlainTextOnly.setVisibility(View.GONE);
        etBody.setText(null);
        etBody.setHint(null);

        grpHeader.setVisibility(View.GONE);
        grpExtra.setVisibility(View.GONE);
        ibCcBcc.setVisibility(View.GONE);
        grpAttachments.setVisibility(View.GONE);
        tvNoInternet.setVisibility(View.GONE);
        grpBody.setVisibility(View.GONE);
        grpSignature.setVisibility(View.GONE);
        grpReferenceHint.setVisibility(View.GONE);
        ibWriteAboveBelow.setVisibility(View.GONE);
        tvLanguage.setVisibility(View.GONE);
        ibReferenceEdit.setVisibility(View.GONE);
        ibReferenceImages.setVisibility(View.GONE);
        tvReference.setVisibility(View.GONE);
        etSearch.setVisibility(View.GONE);
        style_bar.setVisibility(View.GONE);
        media_bar.setVisibility(View.GONE);
        bottom_navigation.setVisibility(View.GONE);
        pbWait.setVisibility(View.VISIBLE);

        invalidateOptionsMenu();
        Helper.setViewsEnabled(view, false);

        final DB db = DB.getInstance(getContext());

        SimpleCursorAdapter cadapter = new SimpleCursorAdapter(
                getContext(),
                R.layout.spinner_contact,
                null,
                new String[]{"name", "email", "photo"},
                new int[]{R.id.tvName, R.id.tvEmail, R.id.ivPhoto},
                0);

        cadapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            private int colName = -1;
            private int colLocal = -1;

            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                try {
                    int id = view.getId();
                    if (id == R.id.tvName) {
                        if (colName < 0)
                            colName = cursor.getColumnIndex("name");

                        if (cursor.isNull(colName)) {
                            ((TextView) view).setText("-");
                            return true;
                        }
                    } else if (id == R.id.ivPhoto) {
                        if (colLocal < 0)
                            colLocal = cursor.getColumnIndex("local");

                        ImageView photo = (ImageView) view;

                        GradientDrawable bg = new GradientDrawable();
                        if (circular)
                            bg.setShape(GradientDrawable.OVAL);
                        else
                            bg.setCornerRadius(dp3);
                        photo.setBackground(bg);
                        photo.setClipToOutline(true);

                        if (cursor.getInt(colLocal) == 1)
                            photo.setImageDrawable(null);
                        else {
                            String uri = cursor.getString(columnIndex);
                            if (uri == null)
                                photo.setImageResource(R.drawable.twotone_person_24);
                            else
                                photo.setImageURI(Uri.parse(uri));
                        }
                        return true;
                    }
                } catch (Throwable ex) {
                    Log.e(ex);
                }
                return false;
            }
        });

        cadapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
            private int colName = -1;
            private int colEmail = -1;

            public CharSequence convertToString(Cursor cursor) {
                try {
                    if (colName < 0)
                        colName = cursor.getColumnIndex("name");
                    if (colEmail < 0)
                        colEmail = cursor.getColumnIndex("email");

                    String name = cursor.getString(colName);
                    String email = MessageHelper.sanitizeEmail(cursor.getString(colEmail));
                    if (name == null || !suggest_names)
                        return email;
                    else {
                        Address address = new InternetAddress(email, name, StandardCharsets.UTF_8.name());
                        return MessageHelper.formatAddressesCompose(new Address[]{address});
                    }
                } catch (Throwable ex) {
                    Log.e(ex);
                    return ex.toString();
                }
            }
        });

        cadapter.setFilterQueryProvider(new FilterQueryProvider() {
            public Cursor runQuery(CharSequence typed) {
                MatrixCursor result = new MatrixCursor(new String[]{"_id", "name", "email", "photo", "local"});

                try {
                    Log.i("Suggest contact=" + typed);
                    if (typed == null)
                        return result;

                    final Context context = getContext();
                    if (context == null)
                        return result;

                    String wildcard = "%" + typed + "%";
                    Map<String, EntityContact> map = new HashMap<>();

                    String glob = "*" +
                            typed.toString().toLowerCase()
                                    .replaceAll("[aáàäâãåæ]", "\\[aáàäâãåæ\\]")
                                    .replaceAll("[bß]", "\\[bß\\]")
                                    .replaceAll("[cç]", "\\[cç\\]")
                                    .replaceAll("[eéèëê]", "\\[eéèëê\\]")
                                    .replaceAll("[iíìïî]", "\\[iíìïî\\]")
                                    .replaceAll("[nñ]", "\\[nñ\\]")
                                    .replaceAll("[oóòöôõøœ]", "\\[oóòöôõøœ\\]")
                                    .replaceAll("[uúùüû]", "\\[uúùüû\\]")
                                    .replace("*", "[*]")
                                    .replace("?", "[?]") +
                            "*";

                    boolean contacts = Helper.hasPermission(context, Manifest.permission.READ_CONTACTS);
                    if (contacts) {
                        try (Cursor cursor = resolver.query(
                                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                                new String[]{
                                        ContactsContract.Contacts.DISPLAY_NAME,
                                        ContactsContract.CommonDataKinds.Email.DATA,
                                        ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                                        ContactsContract.Contacts.STARRED
                                },
                                ContactsContract.CommonDataKinds.Email.DATA + " <> ''" +
                                        " AND (" + ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?" +
                                        " OR LOWER(" + ContactsContract.Contacts.DISPLAY_NAME + ") GLOB ?" +
                                        " OR " + ContactsContract.CommonDataKinds.Email.DATA + " LIKE ?)",
                                new String[]{wildcard, glob, wildcard},
                                null)) {

                            while (cursor != null && cursor.moveToNext()) {
                                EntityContact item = new EntityContact();
                                item.id = 0L;
                                item.name = cursor.getString(0);
                                item.email = cursor.getString(1);
                                item.avatar = cursor.getString(2);
                                item.times_contacted = (cursor.getInt(3) == 0 ? 0 : Integer.MAX_VALUE);
                                item.last_contacted = 0L;
                                EntityContact existing = map.get(item.email);
                                if (existing == null ||
                                        (existing.avatar == null && item.avatar != null))
                                    map.put(item.email, item);
                            }
                        }
                    }

                    List<EntityContact> items = new ArrayList<>();
                    if (suggest_sent)
                        items.addAll(db.contact().searchContacts(null, EntityContact.TYPE_TO, wildcard));
                    if (suggest_received)
                        items.addAll(db.contact().searchContacts(null, EntityContact.TYPE_FROM, wildcard));
                    for (EntityContact item : items) {
                        EntityContact existing = map.get(item.email);
                        if (existing == null)
                            map.put(item.email, item);
                        else {
                            existing.times_contacted = Math.max(existing.times_contacted, item.times_contacted);
                            existing.last_contacted = Math.max(existing.last_contacted, item.last_contacted);
                        }
                    }

                    items = new ArrayList<>(map.values());

                    final Collator collator = Collator.getInstance(Locale.getDefault());
                    collator.setStrength(Collator.SECONDARY); // Case insensitive, process accents etc

                    Collections.sort(items, new Comparator<EntityContact>() {
                        @Override
                        public int compare(EntityContact i1, EntityContact i2) {
                            try {
                                if (suggest_frequently) {
                                    int t = -i1.times_contacted.compareTo(i2.times_contacted);
                                    if (t != 0)
                                        return t;

                                    int l = -i1.last_contacted.compareTo(i2.last_contacted);
                                    if (l != 0)
                                        return l;
                                } else {
                                    // Prefer Android contacts
                                    int a = -Boolean.compare(i1.id == 0, i2.id == 0);
                                    if (a != 0)
                                        return a;
                                }

                                if (TextUtils.isEmpty(i1.name) && TextUtils.isEmpty(i2.name))
                                    return 0;
                                if (TextUtils.isEmpty(i1.name) && !TextUtils.isEmpty(i2.name))
                                    return 1;
                                if (!TextUtils.isEmpty(i1.name) && TextUtils.isEmpty(i2.name))
                                    return -1;

                                int n = collator.compare(i1.name, i2.name);
                                if (n != 0)
                                    return n;

                                return collator.compare(i1.email, i2.email);
                            } catch (Throwable ex) {
                                Log.e(ex);
                                return 0;
                            }
                        }
                    });

                    for (int i = 0; i < items.size(); i++) {
                        EntityContact item = items.get(i);
                        result.newRow()
                                .add(i + 1) // id
                                .add(item.name)
                                .add(item.email)
                                .add(item.avatar)
                                .add(item.id == 0 ? 0 : 1);
                    }
                } catch (Throwable ex) {
                    Log.e(ex);
                }

                Log.i("Suggesting contacts=" + result.getCount());
                return result;
            }
        });

        etTo.setAdapter(cadapter);
        etCc.setAdapter(cadapter);
        etBcc.setAdapter(cadapter);

        etTo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Cursor cursor = (Cursor) adapterView.getAdapter().getItem(position);
                int colEmail = cursor.getColumnIndex("email");
                selectIdentityForEmail(colEmail < 0 ? null : cursor.getString(colEmail));
            }
        });

        grpAddresses.setVisibility(cc_bcc ? View.VISIBLE : View.GONE);

        ibRemoveAttachments.setVisibility(View.GONE);
        ibRemoveAttachments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putString("question", getString(R.string.title_ask_delete_attachments));

                FragmentDialogAsk fragment = new FragmentDialogAsk();
                fragment.setArguments(args);
                fragment.setTargetFragment(FragmentCompose.this, REQUEST_REMOVE_ATTACHMENTS);
                fragment.show(getParentFragmentManager(), "compose:discard");
            }
        });

        rvAttachment.setHasFixedSize(false);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        rvAttachment.setLayoutManager(llm);
        rvAttachment.setItemAnimator(null);

        adapter = new AdapterAttachment(this, false, null);
        rvAttachment.setAdapter(adapter);

        tvNoInternetAttachments.setVisibility(View.GONE);

        return view;
    }

    private void selectIdentityForEmail(String email) {
        if (TextUtils.isEmpty(email))
            return;

        Bundle args = new Bundle();
        args.putString("email", email);

        new SimpleTask<Long>() {
            @Override
            protected Long onExecute(Context context, Bundle args) throws Throwable {
                String email = args.getString("email");

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                boolean auto_identity = prefs.getBoolean("auto_identity", false);
                boolean suggest_sent = prefs.getBoolean("suggest_sent", true);
                boolean suggest_received = prefs.getBoolean("suggest_received", false);

                if (!auto_identity)
                    return null;

                List<Integer> types = new ArrayList<>();
                if (suggest_sent)
                    types.add(EntityContact.TYPE_TO);
                if (suggest_received)
                    types.add(EntityContact.TYPE_FROM);

                if (types.size() == 0)
                    return null;

                DB db = DB.getInstance(context);
                List<Long> identities = db.contact().getIdentities(email, types);
                if (identities != null && identities.size() == 1)
                    return identities.get(0);

                return null;
            }

            @Override
            protected void onExecuted(Bundle args, Long identity) {
                if (identity == null)
                    return;

                SpinnerAdapter adapter = spIdentity.getAdapter();
                for (int pos = 0; pos < adapter.getCount(); pos++) {
                    EntityIdentity item = (EntityIdentity) adapter.getItem(pos);
                    if (item.id.equals(identity)) {
                        spIdentity.setSelection(pos);
                        break;
                    }
                }
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.execute(FragmentCompose.this, args, "compose:contact");
    }

    private void onReferenceEdit() {
        PopupMenuLifecycle popupMenu = new PopupMenuLifecycle(getContext(), getViewLifecycleOwner(), ibReferenceEdit);

        popupMenu.getMenu().add(Menu.NONE, R.string.title_edit_plain_text, 1, R.string.title_edit_plain_text);
        popupMenu.getMenu().add(Menu.NONE, R.string.title_edit_formatted_text, 2, R.string.title_edit_formatted_text);
        popupMenu.getMenu().add(Menu.NONE, R.string.title_clipboard_copy, 3, R.string.title_clipboard_copy);
        popupMenu.getMenu().add(Menu.NONE, R.string.title_delete, 4, R.string.title_delete);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.string.title_edit_plain_text) {
                    convertRef(true);
                    return true;
                } else if (itemId == R.string.title_edit_formatted_text) {
                    convertRef(false);
                    return true;
                } else if (itemId == R.string.title_clipboard_copy) {
                    copyRef();
                    return true;
                } else if (itemId == R.string.title_delete) {
                    deleteRef();
                    return true;
                }
                return false;
            }

            private void convertRef(boolean plain) {
                etBody.clearComposingText();

                Bundle args = new Bundle();
                args.putLong("id", working);
                args.putBoolean("plain", plain);
                args.putString("body", HtmlHelper.toHtml(etBody.getText(), getContext()));

                new SimpleTask<String>() {
                    @Override
                    protected void onPreExecute(Bundle args) {
                        ibReferenceEdit.setEnabled(false);
                    }

                    @Override
                    protected void onPostExecute(Bundle args) {
                        ibReferenceEdit.setEnabled(true);
                    }

                    @Override
                    protected String onExecute(Context context, Bundle args) throws Throwable {
                        long id = args.getLong("id");
                        boolean plain = args.getBoolean("plain");
                        String body = args.getString("body");

                        File rfile = EntityMessage.getFile(context, id);
                        Document doc = JsoupEx.parse(rfile);
                        Elements ref = doc.select("div[fairemail=reference]");
                        ref.removeAttr("fairemail");

                        Document document = JsoupEx.parse(body);
                        if (plain) {
                            String text = HtmlHelper.getText(context, ref.outerHtml());
                            String[] line = text.split("\\r?\\n");
                            for (int i = 0; i < line.length; i++)
                                line[i] = Html.escapeHtml(line[i]);
                            Element p = document.createElement("p");
                            p.html(TextUtils.join("<br>", line));
                            document.body().appendChild(p);
                            return document.html();
                        } else {
                            for (Element element : ref)
                                document.body().appendChild(element);
                            return document.html(); // Edit-ref
                        }
                    }

                    @Override
                    protected void onExecuted(Bundle args, String html) {
                        Bundle extras = new Bundle();
                        extras.putString("html", html);
                        extras.putBoolean("show", true);
                        extras.putBoolean("refedit", true);
                        onAction(R.id.action_save, extras, "refedit");
                    }

                    @Override
                    protected void onException(Bundle args, Throwable ex) {
                        Log.unexpectedError(getParentFragmentManager(), ex);
                    }
                }.setExecutor(executor).execute(FragmentCompose.this, args, "compose:convert");
            }

            private void copyRef() {
                Context context = getContext();
                if (context == null)
                    return;

                ClipboardManager clipboard = Helper.getSystemService(context, ClipboardManager.class);
                if (clipboard == null)
                    return;

                String html = HtmlHelper.toHtml((Spanned) tvReference.getText(), context);

                ClipData clip = ClipData.newHtmlText(
                        etSubject.getText().toString(),
                        HtmlHelper.getText(getContext(), html),
                        html);
                clipboard.setPrimaryClip(clip);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                    ToastEx.makeText(context, R.string.title_clipboard_copied, Toast.LENGTH_LONG).show();
            }

            private void deleteRef() {
                etBody.clearComposingText();

                Bundle extras = new Bundle();
                extras.putString("html", HtmlHelper.toHtml(etBody.getText(), getContext()));
                extras.putBoolean("show", true);
                onAction(R.id.action_save, extras, "refdelete");
            }
        });

        popupMenu.show();
    }

    private void onReferenceImages() {
        show_images = true;
        Bundle extras = new Bundle();
        extras.putBoolean("show", true);
        onAction(R.id.action_save, extras, "refimages");
    }

    @Override
    public void onDestroyView() {
        adapter = null;
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong("fair:working", working);
        outState.putBoolean("fair:show_images", show_images);
        outState.putParcelable("fair:photo", photoURI);

        outState.putInt("fair:pickRequest", pickRequest);
        outState.putParcelable("fair:pickUri", pickUri);

        // Focus was lost at this point
        outState.putInt("fair:selection", etBody == null ? 0 : etBody.getSelectionStart());

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        state = State.NONE;

        try {
            if (savedInstanceState == null) {
                if (working < 0) {
                    Bundle a = getArguments();
                    if (a == null) {
                        a = new Bundle();
                        a.putString("action", "new");
                        setArguments(a);
                    }

                    Bundle args = new Bundle();

                    args.putString("action", a.getString("action"));
                    args.putLong("id", a.getLong("id", -1));
                    args.putLong("account", a.getLong("account", -1));
                    args.putLong("identity", a.getLong("identity", -1));
                    args.putLong("reference", a.getLong("reference", -1));
                    args.putInt("dsn", a.getInt("dsn", -1));
                    args.putSerializable("ics", a.getSerializable("ics"));
                    args.putString("status", a.getString("status"));
                    args.putBoolean("raw", a.getBoolean("raw", false));
                    args.putLong("answer", a.getLong("answer", -1));
                    args.putString("to", a.getString("to"));
                    args.putString("cc", a.getString("cc"));
                    args.putString("bcc", a.getString("bcc"));
                    args.putString("inreplyto", a.getString("inreplyto"));
                    args.putString("subject", a.getString("subject"));
                    args.putString("body", a.getString("body"));
                    args.putString("text", a.getString("text"));
                    args.putCharSequence("selected", a.getCharSequence("selected"));

                    if (a.containsKey("attachments")) {
                        args.putParcelableArrayList("attachments", a.getParcelableArrayList("attachments"));
                        a.remove("attachments");
                        setArguments(a);
                    }

                    draftLoader.execute(FragmentCompose.this, args, "compose:new");
                } else {
                    Bundle args = new Bundle();
                    args.putString("action", "edit");
                    args.putLong("id", working);

                    draftLoader.execute(FragmentCompose.this, args, "compose:edit");
                }
            } else {
                working = savedInstanceState.getLong("fair:working");
                show_images = savedInstanceState.getBoolean("fair:show_images");
                photoURI = savedInstanceState.getParcelable("fair:photo");

                pickRequest = savedInstanceState.getInt("fair:pickRequest");
                pickUri = savedInstanceState.getParcelable("fair:pickUri");

                Bundle args = new Bundle();
                args.putString("action", working < 0 ? "new" : "edit");
                args.putLong("id", working);

                args.putInt("selection", savedInstanceState.getInt("fair:selection"));

                draftLoader.execute(FragmentCompose.this, args, "compose:instance");
            }
        } catch (Throwable ex) {
            Log.e(ex);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        ConnectivityManager cm = Helper.getSystemService(getContext(), ConnectivityManager.class);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        cm.registerNetworkCallback(builder.build(), networkCallback);
    }

    @Override
    public void onPause() {
        final Context context = getContext();

        if (state == State.LOADED) {
            Bundle extras = new Bundle();
            extras.putBoolean("autosave", true);
            onAction(R.id.action_save, extras, "pause");
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putLong("last_composing", working).apply();

        ConnectivityManager cm = Helper.getSystemService(context, ConnectivityManager.class);
        cm.unregisterNetworkCallback(networkCallback);

        super.onPause();
    }

    ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            check();
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            check();
        }

        @Override
        public void onLost(Network network) {
            check();
        }

        private void check() {
            getMainHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                        checkInternet();
                }
            });
        }
    };

    private void checkInternet() {
        boolean suitable = ConnectionHelper.getNetworkState(getContext()).isSuitable();

        Boolean content = (Boolean) tvNoInternet.getTag();
        tvNoInternet.setVisibility(!suitable && content != null && !content ? View.VISIBLE : View.GONE);

        Boolean downloading = (Boolean) rvAttachment.getTag();
        tvNoInternetAttachments.setVisibility(!suitable && downloading != null && downloading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_compose, menu);

        final Context context = getContext();
        PopupMenuLifecycle.insertIcons(context, menu, false);

        LayoutInflater infl = LayoutInflater.from(context);

        View v = infl.inflate(R.layout.action_button_text, null);
        v.setId(View.generateViewId());
        ImageButton ib = v.findViewById(R.id.button);
        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMenuEncrypt();
            }
        });
        ib.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int[] pos = new int[2];
                ib.getLocationOnScreen(pos);
                int dp24 = Helper.dp2pixels(v.getContext(), 24);

                Toast toast = ToastEx.makeTextBw(v.getContext(),
                        getString(R.string.title_encrypt), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP | Gravity.START, pos[0], pos[1] + dp24);
                toast.show();
                return true;
            }
        });
        menu.findItem(R.id.menu_encrypt).setActionView(v);

        ImageButton ibTranslate = (ImageButton) infl.inflate(R.layout.action_button, null);
        ibTranslate.setId(View.generateViewId());
        ibTranslate.setImageResource(R.drawable.twotone_translate_24);
        ib.setContentDescription(getString(R.string.title_translate));
        ibTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onTranslate(vwAnchorMenu);
            }
        });
        menu.findItem(R.id.menu_translate).setActionView(ibTranslate);

        ImageButton ibZoom = (ImageButton) infl.inflate(R.layout.action_button, null);
        ibZoom.setId(View.generateViewId());
        ibZoom.setImageResource(R.drawable.twotone_format_size_24);
        ib.setContentDescription(getString(R.string.title_legend_zoom));
        ibZoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onMenuZoom();
            }
        });
        menu.findItem(R.id.menu_zoom).setActionView(ibZoom);

        MenuCompat.setGroupDividerEnabled(menu, true);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final Context context = getContext();

        menu.findItem(R.id.menu_encrypt).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_translate).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_translate).setVisible(DeepL.isAvailable(context));
        menu.findItem(R.id.menu_zoom).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_media).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_compact).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_contact_group).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_manage_local_contacts).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_answer_insert).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_answer_create).setEnabled(state == State.LOADED);
        menu.findItem(R.id.menu_clear).setEnabled(state == State.LOADED);

        SpannableStringBuilder ssbZoom = new SpannableStringBuilder(getString(R.string.title_zoom));
        ssbZoom.append(' ');
        for (int i = 0; i <= zoom; i++)
            ssbZoom.append('+');
        menu.findItem(R.id.menu_zoom).setTitle(ssbZoom);
        PopupMenuLifecycle.insertIcon(context, menu.findItem(R.id.menu_zoom), false);

        ActionBar actionBar = getSupportActionBar();
        Context actionBarContext = (actionBar == null ? context : actionBar.getThemedContext());
        int colorEncrypt = Helper.resolveColor(context, R.attr.colorEncrypt);
        int colorActionForeground = Helper.resolveColor(actionBarContext, android.R.attr.textColorPrimary);

        View v = menu.findItem(R.id.menu_encrypt).getActionView();
        ImageButton ib = v.findViewById(R.id.button);
        TextView tv = v.findViewById(R.id.text);

        v.setAlpha(state == State.LOADED && !dsn ? 1f : Helper.LOW_LIGHT);
        ib.setEnabled(state == State.LOADED && !dsn);

        if (EntityMessage.PGP_SIGNONLY.equals(encrypt) || EntityMessage.SMIME_SIGNONLY.equals(encrypt)) {
            ib.setImageResource(R.drawable.twotone_gesture_24);
            ib.setImageTintList(ColorStateList.valueOf(colorActionForeground));
            tv.setText(EntityMessage.PGP_SIGNONLY.equals(encrypt) ? "P" : "S");
        } else if (EntityMessage.PGP_SIGNENCRYPT.equals(encrypt) || EntityMessage.SMIME_SIGNENCRYPT.equals(encrypt)) {
            ib.setImageResource(R.drawable.twotone_lock_24);
            ib.setImageTintList(ColorStateList.valueOf(colorEncrypt));
            tv.setText(EntityMessage.PGP_SIGNENCRYPT.equals(encrypt) ? "P" : "S");
        } else {
            ib.setImageResource(R.drawable.twotone_lock_open_24);
            ib.setImageTintList(ColorStateList.valueOf(colorActionForeground));
            tv.setText(null);
        }

        ImageButton ibTranslate = (ImageButton) menu.findItem(R.id.menu_translate).getActionView();
        ibTranslate.setAlpha(state == State.LOADED ? 1f : Helper.LOW_LIGHT);
        ibTranslate.setEnabled(state == State.LOADED);

        ImageButton ibZoom = (ImageButton) menu.findItem(R.id.menu_zoom).getActionView();
        ibZoom.setAlpha(state == State.LOADED ? 1f : Helper.LOW_LIGHT);
        ibZoom.setEnabled(state == State.LOADED);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean save_drafts = prefs.getBoolean("save_drafts", true);
        boolean send_chips = prefs.getBoolean("send_chips", true);
        boolean send_dialog = prefs.getBoolean("send_dialog", true);
        boolean image_dialog = prefs.getBoolean("image_dialog", true);

        menu.findItem(R.id.menu_save_drafts).setChecked(save_drafts);
        menu.findItem(R.id.menu_send_chips).setChecked(send_chips);
        menu.findItem(R.id.menu_send_dialog).setChecked(send_dialog);
        menu.findItem(R.id.menu_image_dialog).setChecked(image_dialog);
        menu.findItem(R.id.menu_media).setChecked(media);
        menu.findItem(R.id.menu_compact).setChecked(compact);

        View image = media_bar.findViewById(R.id.menu_image);
        if (image != null)
            image.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onActionImage(false, true);
                    return true;
                }
            });

        if (EntityMessage.PGP_SIGNONLY.equals(encrypt) ||
                EntityMessage.SMIME_SIGNONLY.equals(encrypt))
            bottom_navigation.getMenu().findItem(R.id.action_send).setTitle(R.string.title_sign);
        else if (EntityMessage.PGP_SIGNENCRYPT.equals(encrypt) ||
                EntityMessage.SMIME_SIGNENCRYPT.equals(encrypt))
            bottom_navigation.getMenu().findItem(R.id.action_send).setTitle(R.string.title_encrypt);
        else
            bottom_navigation.getMenu().findItem(R.id.action_send).setTitle(R.string.title_send);

        Menu m = bottom_navigation.getMenu();
        for (int i = 0; i < m.size(); i++)
            bottom_navigation.findViewById(m.getItem(i).getItemId()).setOnLongClickListener(null);

        bottom_navigation.findViewById(R.id.action_save).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (LanguageTool.isEnabled(v.getContext())) {
                    onLanguageTool();
                    return true;
                } else
                    return false;
            }
        });

        bottom_navigation.findViewById(R.id.action_send).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Bundle args = new Bundle();
                args.putBoolean("force_dialog", true);
                onAction(R.id.action_check, args, "force");
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_encrypt) {
            onMenuEncrypt();
            return true;
        } else if (itemId == R.id.menu_translate) {
            onTranslate(vwAnchorMenu);
            return true;
        } else if (itemId == R.id.menu_zoom) {
            onMenuZoom();
            return true;
        } else if (itemId == R.id.menu_save_drafts) {
            onMenuSaveDrafts();
            return true;
        } else if (itemId == R.id.menu_send_chips) {
            onMenuSendChips();
            return true;
        } else if (itemId == R.id.menu_send_dialog) {
            onMenuSendDialog();
            return true;
        } else if (itemId == R.id.menu_image_dialog) {
            onMenuImageDialog();
            return true;
        } else if (itemId == R.id.menu_media) {
            onMenuMediaBar();
            return true;
        } else if (itemId == R.id.menu_compact) {
            onMenuCompact();
            return true;
        } else if (itemId == R.id.menu_contact_group) {
            onMenuContactGroup();
            return true;
        } else if (itemId == R.id.menu_manage_local_contacts) {
            onMenuManageLocalContacts();
            return true;
        } else if (itemId == R.id.menu_answer_insert) {
            onMenuAnswerInsert();
            return true;
        } else if (itemId == R.id.menu_answer_create) {
            onMenuAnswerCreate();
            return true;
        } else if (itemId == R.id.title_search_in_text) {
            startSearch();
            return true;
        } else if (itemId == R.id.menu_clear) {
            StyleHelper.apply(R.id.menu_clear, getViewLifecycleOwner(), null, etBody);
            return true;
        } else if (itemId == R.id.menu_legend) {
            onMenuLegend();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onMenuAddresses() {
        grpAddresses.setVisibility(grpAddresses.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);

        getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                    return;
                if (grpAddresses.getVisibility() == View.GONE)
                    etSubject.requestFocus();
                else
                    etCc.requestFocus();
            }
        });
    }

    private void onMenuEncrypt() {
        EntityIdentity identity = (EntityIdentity) spIdentity.getSelectedItem();
        if (identity == null || identity.encrypt == 0) {
            if (EntityMessage.ENCRYPT_NONE.equals(encrypt) || encrypt == null)
                encrypt = EntityMessage.PGP_SIGNENCRYPT;
            else if (EntityMessage.PGP_SIGNENCRYPT.equals(encrypt))
                encrypt = EntityMessage.PGP_SIGNONLY;
            else
                encrypt = EntityMessage.ENCRYPT_NONE;
        } else {
            if (EntityMessage.ENCRYPT_NONE.equals(encrypt) || encrypt == null)
                encrypt = EntityMessage.SMIME_SIGNENCRYPT;
            else if (EntityMessage.SMIME_SIGNENCRYPT.equals(encrypt))
                encrypt = EntityMessage.SMIME_SIGNONLY;
            else
                encrypt = EntityMessage.ENCRYPT_NONE;
        }

        invalidateOptionsMenu();

        Bundle args = new Bundle();
        args.putLong("id", working);
        args.putInt("encrypt", encrypt);

        new SimpleTask<Void>() {
            @Override
            protected Void onExecute(Context context, Bundle args) {
                long id = args.getLong("id");
                int encrypt = args.getInt("encrypt");

                DB db = DB.getInstance(context);
                if (EntityMessage.ENCRYPT_NONE.equals(encrypt))
                    db.message().setMessageUiEncrypt(id, null);
                else
                    db.message().setMessageUiEncrypt(id, encrypt);

                return null;
            }

            @Override
            protected void onExecuted(Bundle args, Void data) {
                int encrypt = args.getInt("encrypt");
                int[] values = getResources().getIntArray(R.array.encryptValues);
                String[] names = getResources().getStringArray(R.array.encryptNames);
                for (int i = 0; i < values.length; i++)
                    if (values[i] == encrypt) {
                        ToastEx.makeText(getContext(), names[i], Toast.LENGTH_LONG).show();
                        break;
                    }
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.setExecutor(executor).execute(this, args, "compose:encrypt");
    }

    private void onMenuZoom() {
        zoom = ++zoom % 3;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putInt("compose_zoom", zoom).apply();
        setZoom();
    }

    private void setZoom() {
        final Context context = getContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int message_zoom = prefs.getInt("message_zoom", 100);
        float textSize = Helper.getTextSize(context, zoom);
        if (textSize != 0) {
            etBody.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize * message_zoom / 100f);
            tvReference.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize * message_zoom / 100f);
        }
    }

    private void onMenuSaveDrafts() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean save_drafts = prefs.getBoolean("save_drafts", true);
        prefs.edit().putBoolean("save_drafts", !save_drafts).apply();
    }

    private void onMenuSendChips() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean send_chips = prefs.getBoolean("send_chips", true);
        prefs.edit().putBoolean("send_chips", !send_chips).apply();

        etTo.setText(etTo.getText());
        etCc.setText(etCc.getText());
        etBcc.setText(etBcc.getText());

        etSubject.requestFocus();
    }

    private void onMenuSendDialog() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean send_dialog = prefs.getBoolean("send_dialog", true);
        prefs.edit().putBoolean("send_dialog", !send_dialog).apply();
    }

    private void onMenuImageDialog() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean image_dialog = prefs.getBoolean("image_dialog", true);
        prefs.edit().putBoolean("image_dialog", !image_dialog).apply();
    }

    private void onMenuMediaBar() {
        media = !media;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putBoolean("compose_media", media).apply();
        etBody.setSelection(etBody.getSelectionStart());
        media_bar.getMenu().clear();
        media_bar.inflateMenu(R.menu.action_compose_media);
        media_bar.setVisibility(media ? View.VISIBLE : View.GONE);
        style_bar.setVisibility(View.GONE);
        invalidateOptionsMenu();
    }

    private void onMenuCompact() {
        compact = !compact;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putBoolean("compose_compact", compact).apply();
        setCompact(compact);
    }

    private void setCompact(boolean compact) {
        bottom_navigation.setLabelVisibilityMode(compact
                ? LabelVisibilityMode.LABEL_VISIBILITY_UNLABELED
                : LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
        ViewGroup.LayoutParams params = bottom_navigation.getLayoutParams();
        params.height = Helper.dp2pixels(view.getContext(), compact ? 36 : 56);
        bottom_navigation.setLayoutParams(params);
    }

    private void onMenuLegend() {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
            getParentFragmentManager().popBackStack("legend", FragmentManager.POP_BACK_STACK_INCLUSIVE);

        Bundle args = new Bundle();
        args.putString("tab", "compose");

        Fragment fragment = new FragmentLegend();
        fragment.setArguments(args);

        FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, fragment).addToBackStack("legend");
        fragmentTransaction.commit();
    }

    private void onMenuContactGroup() {
        onMenuContactGroup(view.findFocus());
    }

    private void onMenuManageLocalContacts() {
        FragmentContacts fragment = new FragmentContacts();
        fragment.setArguments(new Bundle()); // all accounts

        FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, fragment).addToBackStack("contacts");
        fragmentTransaction.commit();
    }

    private void onMenuContactGroup(View v) {
        int focussed = 0;
        if (v != null) {
            if (v.getId() == R.id.etCc)
                focussed = 1;
            else if (v.getId() == R.id.etBcc)
                focussed = 2;
        }

        Bundle args = new Bundle();
        args.putLong("working", working);
        args.putInt("focussed", focussed);

        Helper.hideKeyboard(view);

        FragmentDialogContactGroup fragment = new FragmentDialogContactGroup();
        fragment.setArguments(args);
        fragment.setTargetFragment(this, REQUEST_CONTACT_GROUP);
        fragment.show(getParentFragmentManager(), "compose:groups");
    }

    private void onMenuAnswerInsert() {
        new SimpleTask<List<EntityAnswer>>() {
            @Override
            protected List<EntityAnswer> onExecute(Context context, Bundle args) {
                List<EntityAnswer> answers = DB.getInstance(context).answer().getAnswers(false);
                return (answers == null ? new ArrayList<>() : answers);
            }

            @Override
            protected void onExecuted(Bundle args, final List<EntityAnswer> answers) {
                final Context context = getContext();

                if (answers.size() == 0) {
                    ToastEx.makeText(context, R.string.title_no_answers, Toast.LENGTH_LONG).show();
                    return;
                }

                PopupMenuLifecycle popupMenu = new PopupMenuLifecycle(context, getViewLifecycleOwner(), vwAnchorMenu);
                EntityAnswer.fillMenu(popupMenu.getMenu(), true, answers, context);

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem target) {
                        Intent intent = target.getIntent();
                        if (intent == null)
                            return false;

                        if (!ActivityBilling.isPro(context)) {
                            startActivity(new Intent(context, ActivityBilling.class));
                            return true;
                        }

                        if (target.getGroupId() == 999) {
                            CharSequence config = intent.getCharSequenceExtra("config");
                            int start = etBody.getSelectionStart();
                            etBody.getText().insert(start, config);
                            return true;
                        }

                        long id = intent.getLongExtra("id", -1);

                        Bundle args = new Bundle();
                        args.putLong("id", id);

                        new SimpleTask<EntityAnswer>() {
                            @Override
                            protected EntityAnswer onExecute(Context context, Bundle args) throws Throwable {
                                long id = args.getLong("id");

                                DB db = DB.getInstance(context);
                                EntityAnswer answer = db.answer().getAnswer(id);
                                if (answer != null)
                                    db.answer().applyAnswer(answer.id, new Date().getTime());

                                return answer;
                            }

                            @Override
                            protected void onExecuted(Bundle args, EntityAnswer answer) {
                                if (answer == null)
                                    return;

                                if (etSubject.getText().length() == 0)
                                    etSubject.setText(answer.name);

                                InternetAddress[] to = null;
                                try {
                                    to = MessageHelper.parseAddresses(getContext(), etTo.getText().toString());
                                } catch (AddressException ignored) {
                                }

                                String html = EntityAnswer.replacePlaceholders(context, answer.text, to);

                                Spanned spanned = HtmlHelper.fromHtml(html, new HtmlHelper.ImageGetterEx() {
                                    @Override
                                    public Drawable getDrawable(Element element) {
                                        String source = element.attr("src");
                                        if (source.startsWith("cid:"))
                                            element.attr("src", "cid:");
                                        return ImageHelper.decodeImage(getContext(),
                                                working, element, true, zoom, 1.0f, etBody);
                                    }
                                }, null, getContext());

                                int start = etBody.getSelectionStart();
                                int end = etBody.getSelectionEnd();
                                if (start > end) {
                                    int tmp = start;
                                    start = end;
                                    end = tmp;
                                }

                                if (start >= 0 && start < end)
                                    etBody.getText().replace(start, end, spanned);
                                else {
                                    if (start < 0) {
                                        start = etBody.length();
                                        etBody.getText().append(spanned);
                                    } else
                                        etBody.getText().insert(start, spanned);

                                    int pos = getAutoPos(start, spanned.length());
                                    if (pos >= 0)
                                        etBody.setSelection(pos);
                                }

                                StyleHelper.markAsInserted(etBody.getText(), start, start + spanned.length());
                            }

                            @Override
                            protected void onException(Bundle args, Throwable ex) {
                                Log.unexpectedError(getParentFragmentManager(), ex);
                            }
                        }.setExecutor(executor).execute(FragmentCompose.this, args, "compose:answer");

                        return true;
                    }
                });

                popupMenu.show();
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.setExecutor(executor).execute(getContext(), getViewLifecycleOwner(), new Bundle(), "compose:answer");
    }

    private void onMenuAnswerCreate() {
        etBody.clearComposingText();

        Bundle args = new Bundle();
        args.putString("subject", etSubject.getText().toString());
        args.putString("html", HtmlHelper.toHtml(etBody.getText(), getContext()));

        FragmentAnswer fragment = new FragmentAnswer();
        fragment.setArguments(args);
        FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, fragment).addToBackStack("compose:answer");
        fragmentTransaction.commit();
    }

    private void onTranslate(View anchor) {
        final Context context = anchor.getContext();

        boolean grouped = BuildConfig.DEBUG;
        List<DeepL.Language> languages = DeepL.getTargetLanguages(context, grouped);
        if (languages == null)
            languages = new ArrayList<>();

        Pair<Integer, Integer> paragraph = DeepL.getParagraph(etBody);
        boolean canTranslate = (DeepL.canTranslate(context) && paragraph != null);

        PopupMenuLifecycle popupMenu = new PopupMenuLifecycle(context, getViewLifecycleOwner(), anchor);

        popupMenu.getMenu().add(Menu.NONE, 1, 1, R.string.title_translate_configure);

        NumberFormat NF = NumberFormat.getNumberInstance();
        for (int i = 0; i < languages.size(); i++) {
            DeepL.Language lang = languages.get(i);

            SpannableStringBuilder ssb = new SpannableStringBuilderEx(lang.name);
            if (grouped && lang.frequency > 0) {
                int start = ssb.length();
                ssb.append(" (").append(NF.format(lang.frequency)).append(")");
                ssb.setSpan(new RelativeSizeSpan(HtmlHelper.FONT_SMALL),
                        start, ssb.length(), 0);

            }

            MenuItem item = popupMenu.getMenu()
                    .add(lang.favorite ? Menu.FIRST : Menu.NONE, i + 2, i + 2, ssb)
                    .setIntent(new Intent().putExtra("target", lang.target));
            if (lang.icon != null)
                item.setIcon(lang.icon);
            item.setEnabled(canTranslate);
        }

        if (grouped)
            MenuCompat.setGroupDividerEnabled(popupMenu.getMenu(), true);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == 1) {
                    DeepL.FragmentDialogDeepL fragment = new DeepL.FragmentDialogDeepL();
                    fragment.show(getParentFragmentManager(), "deepl:configure");
                } else {
                    String target = item.getIntent().getStringExtra("target");
                    onMenuTranslate(target);
                }
                return true;
            }

            private void onMenuTranslate(String target) {
                final Pair<Integer, Integer> paragraph = DeepL.getParagraph(etBody);
                if (paragraph == null)
                    return;

                etBody.clearComposingText();

                Editable edit = etBody.getText();
                CharSequence text = edit.subSequence(paragraph.first, paragraph.second);

                Bundle args = new Bundle();
                args.putString("target", target);
                args.putCharSequence("text", text);

                new SimpleTask<DeepL.Translation>() {
                    private Object highlightSpan;
                    private Toast toast = null;

                    @Override
                    protected void onPreExecute(Bundle args) {
                        int textColorHighlight = Helper.resolveColor(getContext(), android.R.attr.textColorHighlight);
                        highlightSpan = new BackgroundColorSpan(textColorHighlight);
                        etBody.getText().setSpan(highlightSpan, paragraph.first, paragraph.second,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_COMPOSING);
                        toast = ToastEx.makeText(context, R.string.title_translating, Toast.LENGTH_SHORT);
                        toast.show();
                    }

                    @Override
                    protected void onPostExecute(Bundle args) {
                        if (highlightSpan != null)
                            etBody.getText().removeSpan(highlightSpan);
                        if (toast != null)
                            toast.cancel();
                    }

                    @Override
                    protected DeepL.Translation onExecute(Context context, Bundle args) throws Throwable {
                        String target = args.getString("target");
                        CharSequence text = args.getCharSequence("text");
                        return DeepL.translate(text, true, target, context);
                    }

                    @Override
                    protected void onExecuted(Bundle args, DeepL.Translation translation) {
                        if (paragraph.second > edit.length())
                            return;

                        FragmentActivity activity = getActivity();
                        if (activity == null)
                            return;

                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

                        // Insert translated text
                        /*
                            java.lang.IndexOutOfBoundsException: charAt: -1 < 0
                             at android.text.SpannableStringBuilder.charAt(SpannableStringBuilder.java:123)
                             at java.lang.Character.codePointBefore(Character.java:5002)
                             at android.widget.SpellChecker.spellCheck(SpellChecker.java:317)
                             at android.widget.SpellChecker.access$900(SpellChecker.java:48)
                             at android.widget.SpellChecker$SpellParser.parse(SpellChecker.java:760)
                             at android.widget.SpellChecker$SpellParser.parse(SpellChecker.java:649)
                             at android.widget.SpellChecker.spellCheck(SpellChecker.java:263)
                             at android.widget.SpellChecker.spellCheck(SpellChecker.java:229)
                             at android.widget.Editor.updateSpellCheckSpans(Editor.java:1015)
                             at android.widget.Editor.sendOnTextChanged(Editor.java:1610)
                             at android.widget.TextView.sendOnTextChanged(TextView.java:10793)
                             at android.widget.TextView.handleTextChanged(TextView.java:10904)
                             at android.widget.TextView$ChangeWatcher.onTextChanged(TextView.java:13798)
                             at android.text.SpannableStringBuilder.sendTextChanged(SpannableStringBuilder.java:1268)
                             at android.text.SpannableStringBuilder.replace(SpannableStringBuilder.java:577)
                             at android.text.SpannableStringBuilder.insert(SpannableStringBuilder.java:226)
                             at android.text.SpannableStringBuilder.insert(SpannableStringBuilder.java:38)
                         */
                        int len = 2 + translation.translated_text.length();
                        edit.insert(paragraph.second, translation.translated_text);
                        edit.insert(paragraph.second, "\n\n");
                        StyleHelper.markAsInserted(edit, paragraph.second, paragraph.second + len);

                        etBody.setSelection(paragraph.second + len);

                        boolean small = prefs.getBoolean("deepl_small", false);
                        if (small) {
                            RelativeSizeSpan[] spans = edit.getSpans(
                                    paragraph.first, paragraph.second, RelativeSizeSpan.class);
                            for (RelativeSizeSpan span : spans)
                                edit.removeSpan(span);
                            edit.setSpan(new RelativeSizeSpan(HtmlHelper.FONT_SMALL),
                                    paragraph.first, paragraph.second,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }

                        // Updated frequency
                        String key = "translated_" + args.getString("target");
                        int count = prefs.getInt(key, 0);
                        prefs.edit().putInt(key, count + 1).apply();

                        activity.invalidateOptionsMenu();
                    }

                    @Override
                    protected void onDestroyed(Bundle args) {
                        if (toast != null) {
                            toast.cancel();
                            toast = null;
                        }
                    }

                    @Override
                    protected void onException(Bundle args, Throwable ex) {
                        etBody.setSelection(paragraph.second);
                        Throwable exex = new Throwable("DeepL", ex);
                        Log.unexpectedError(getParentFragmentManager(), exex, false);
                    }
                }.setExecutor(executor).execute(FragmentCompose.this, args, "compose:translate");
            }
        });

        popupMenu.showWithIcons(context, anchor);
    }

    private void onLanguageTool() {
        etBody.clearComposingText();

        Log.i("LT running enabled=" + etBody.isSuggestionsEnabled());

        Bundle args = new Bundle();
        args.putCharSequence("text", etBody.getText());

        new SimpleTask<List<LanguageTool.Suggestion>>() {
            private Toast toast = null;

            @Override
            protected void onPreExecute(Bundle args) {
                toast = ToastEx.makeText(getContext(), R.string.title_suggestions_check, Toast.LENGTH_LONG);
                toast.show();
                setBusy(true);
            }

            @Override
            protected void onPostExecute(Bundle args) {
                if (toast != null)
                    toast.cancel();
                setBusy(false);
            }

            @Override
            protected List<LanguageTool.Suggestion> onExecute(Context context, Bundle args) throws Throwable {
                CharSequence text = args.getCharSequence("text").toString();
                return LanguageTool.getSuggestions(context, text);
            }

            @Override
            protected void onExecuted(Bundle args, List<LanguageTool.Suggestion> suggestions) {
                if (suggestions == null || suggestions.size() == 0) {
                    ToastEx.makeText(getContext(), R.string.title_suggestions_none, Toast.LENGTH_LONG).show();
                    return;
                }

                Editable edit = etBody.getText();
                if (edit == null)
                    return;

                // https://developer.android.com/reference/android/text/style/SuggestionSpan
                for (SuggestionSpanEx span : edit.getSpans(0, edit.length(), SuggestionSpanEx.class)) {
                    Log.i("LT removing=" + span);
                    edit.removeSpan(span);
                }

                for (LanguageTool.Suggestion suggestion : suggestions) {
                    Log.i("LT adding=" + suggestion);
                    SuggestionSpan span = new SuggestionSpanEx(getContext(),
                            suggestion.replacements.toArray(new String[0]),
                            SuggestionSpan.FLAG_MISSPELLED);
                    int start = suggestion.offset;
                    int end = start + suggestion.length;
                    edit.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            @Override
            protected void onDestroyed(Bundle args) {
                if (toast != null) {
                    toast.cancel();
                    toast = null;
                }
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Throwable exex = new Throwable("LanguageTool", ex);
                Log.unexpectedError(getParentFragmentManager(), exex, false);
            }
        }.execute(this, args, "compose:lt");
    }

    private static class SuggestionSpanEx extends SuggestionSpan {
        private final int textColorHighlight;

        public SuggestionSpanEx(Context context, String[] suggestions, int flags) {
            super(context, suggestions, flags);
            textColorHighlight = Helper.resolveColor(context, android.R.attr.textColorHighlight);
        }

        @Override
        public void updateDrawState(TextPaint tp) {
            tp.bgColor = textColorHighlight;
        }
    }

    private boolean onActionStyle(int action, View anchor) {
        Log.i("Style action=" + action);
        return StyleHelper.apply(action, getViewLifecycleOwner(), anchor, etBody);
    }

    private void onActionRecordAudio() {
        // https://developer.android.com/reference/android/provider/MediaStore.Audio.Media.html#RECORD_SOUND_ACTION
        PackageManager pm = getContext().getPackageManager();
        Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        if (intent.resolveActivity(pm) == null) { // action whitelisted
            Snackbar snackbar = Snackbar.make(view, getString(R.string.title_no_recorder), Snackbar.LENGTH_INDEFINITE)
                    .setGestureInsetBottomIgnored(true);
            snackbar.setAction(R.string.title_fix, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Helper.viewFAQ(v.getContext(), 158);
                }
            });
            snackbar.show();
        } else
            try {
                startActivityForResult(intent, REQUEST_RECORD_AUDIO);
            } catch (Throwable ex) {
                Helper.reportNoViewer(getContext(), intent, ex);
            }
    }

    private void onActionImage(boolean photo, boolean force) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean image_dialog = prefs.getBoolean("image_dialog", true);
        if (image_dialog || force) {
            Helper.hideKeyboard(view);

            Bundle args = new Bundle();
            args.putInt("title", photo
                    ? R.string.title_attachment_photo
                    : R.string.title_add_image_select);

            FragmentDialogAddImage fragment = new FragmentDialogAddImage();
            fragment.setArguments(args);
            fragment.setTargetFragment(this, REQUEST_IMAGE);
            fragment.show(getParentFragmentManager(), "compose:image");
        } else
            onAddImage(photo);
    }

    private void onActionAttachment() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        PackageManager pm = getContext().getPackageManager();
        if (intent.resolveActivity(pm) == null) // system whitelisted
            noStorageAccessFramework();
        else
            startActivityForResult(Helper.getChooser(getContext(), intent), REQUEST_ATTACHMENT);
    }

    private void noStorageAccessFramework() {
        Snackbar snackbar = Snackbar.make(view, R.string.title_no_saf, Snackbar.LENGTH_LONG)
                .setGestureInsetBottomIgnored(true);
        snackbar.setAction(R.string.title_fix, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.viewFAQ(v.getContext(), 25);
            }
        });
        snackbar.show();
    }

    private void onActionLink() {
        FragmentDialogInsertLink fragment = new FragmentDialogInsertLink();
        fragment.setArguments(FragmentDialogInsertLink.getArguments(etBody));
        fragment.setTargetFragment(this, REQUEST_LINK);
        fragment.show(getParentFragmentManager(), "compose:link");
    }

    private void onActionDiscard() {
        if (isEmpty())
            onAction(R.id.action_delete, "discard");
        else {
            Bundle args = new Bundle();
            args.putString("question", getString(R.string.title_ask_discard));
            args.putBoolean("warning", true);

            FragmentDialogAsk fragment = new FragmentDialogAsk();
            fragment.setArguments(args);
            fragment.setTargetFragment(this, REQUEST_DISCARD);
            fragment.show(getParentFragmentManager(), "compose:discard");
        }
    }

    private void onEncrypt(final EntityMessage draft, final int action, final Bundle extras, final boolean interactive) {
        if (EntityMessage.SMIME_SIGNONLY.equals(draft.ui_encrypt) ||
                EntityMessage.SMIME_SIGNENCRYPT.equals(draft.ui_encrypt)) {
            Bundle args = new Bundle();
            args.putLong("id", draft.id);
            args.putInt("type", draft.ui_encrypt);

            new SimpleTask<EntityIdentity>() {
                @Override
                protected EntityIdentity onExecute(Context context, Bundle args) {
                    long id = args.getLong("id");

                    DB db = DB.getInstance(context);
                    EntityMessage draft = db.message().getMessage(id);
                    if (draft == null || draft.identity == null)
                        return null;

                    EntityIdentity identity = db.identity().getIdentity(draft.identity);
                    if (identity != null && identity.sign_key_alias != null)
                        try {
                            PrivateKey key = KeyChain.getPrivateKey(context, identity.sign_key_alias);
                            args.putBoolean("available", key != null);
                        } catch (Throwable ex) {
                            Log.w(ex);
                        }

                    return identity;
                }

                @Override
                protected void onExecuted(final Bundle args, EntityIdentity identity) {
                    if (identity == null)
                        return;

                    boolean available = args.getBoolean("available");
                    if (available) {
                        args.putString("alias", identity.sign_key_alias);
                        onSmime(args, action, extras);
                        return;
                    }

                    if (interactive)
                        Helper.selectKeyAlias(getActivity(), getViewLifecycleOwner(), identity.sign_key_alias, new Helper.IKeyAlias() {
                            @Override
                            public void onSelected(String alias) {
                                args.putString("alias", alias);
                                if (alias != null)
                                    onSmime(args, action, extras);
                            }

                            @Override
                            public void onNothingSelected() {
                                Snackbar snackbar = Snackbar.make(view, R.string.title_no_key, Snackbar.LENGTH_LONG)
                                        .setGestureInsetBottomIgnored(true);
                                final Intent intent = (Build.VERSION.SDK_INT < Build.VERSION_CODES.R
                                        ? KeyChain.createInstallIntent()
                                        : new Intent(Settings.ACTION_SECURITY_SETTINGS));
                                PackageManager pm = getContext().getPackageManager();
                                if (intent.resolveActivity(pm) != null) // package whitelisted
                                    snackbar.setAction(R.string.title_fix, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            startActivity(intent);
                                        }
                                    });
                                snackbar.show();
                            }
                        });
                }

                @Override
                protected void onException(Bundle args, Throwable ex) {
                    Log.unexpectedError(getParentFragmentManager(), ex);
                }
            }.setExecutor(executor).execute(this, args, "compose:alias");
        } else {
            try {
                List<Address> recipients = new ArrayList<>();
                if (draft.from != null)
                    recipients.addAll(Arrays.asList(draft.from));
                if (draft.to != null)
                    recipients.addAll(Arrays.asList(draft.to));
                if (draft.cc != null)
                    recipients.addAll(Arrays.asList(draft.cc));
                if (draft.bcc != null)
                    recipients.addAll(Arrays.asList(draft.bcc));

                if (recipients.size() == 0)
                    throw new IllegalArgumentException(getString(R.string.title_to_missing));

                List<String> emails = new ArrayList<>();
                for (int i = 0; i < recipients.size(); i++) {
                    InternetAddress recipient = (InternetAddress) recipients.get(i);
                    String email = recipient.getAddress();
                    if (!emails.contains(email))
                        emails.add(email);
                }
                pgpUserIds = emails.toArray(new String[0]);

                Intent intent;
                if (EntityMessage.PGP_SIGNONLY.equals(draft.ui_encrypt))
                    intent = new Intent(OpenPgpApi.ACTION_GET_SIGN_KEY_ID);
                else if (EntityMessage.PGP_SIGNENCRYPT.equals(draft.ui_encrypt)) {
                    intent = new Intent(OpenPgpApi.ACTION_GET_KEY_IDS);
                    intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, pgpUserIds);
                } else
                    throw new IllegalArgumentException("Invalid encrypt=" + draft.ui_encrypt);

                Bundle largs = new Bundle();
                largs.putLong("id", working);
                largs.putString("session", UUID.randomUUID().toString());
                largs.putInt("action", action);
                largs.putBundle("extras", extras);
                largs.putBoolean("interactive", interactive);
                intent.putExtra(BuildConfig.APPLICATION_ID, largs);

                onPgp(intent);
            } catch (Throwable ex) {
                if (ex instanceof IllegalArgumentException)
                    Snackbar.make(view, ex.getMessage(), Snackbar.LENGTH_LONG)
                            .setGestureInsetBottomIgnored(true).show();
                else {
                    Log.e(ex);
                    Log.unexpectedError(getParentFragmentManager(), ex);
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            switch (requestCode) {
                case REQUEST_CONTACT_TO:
                case REQUEST_CONTACT_CC:
                case REQUEST_CONTACT_BCC:
                    if (resultCode == RESULT_OK && data != null)
                        onPickContact(requestCode, data);
                    break;
                case REQUEST_SHARED:
                    if (resultCode == RESULT_OK && data != null) {
                        Bundle args = data.getBundleExtra("args");
                        onAddImageFile(args.getParcelableArrayList("images"));
                    }
                    break;
                case REQUEST_IMAGE:
                    if (resultCode == RESULT_OK) {
                        int title = data.getBundleExtra("args").getInt("title");
                        onAddImage(title == R.string.title_attachment_photo);
                    }
                    break;
                case REQUEST_IMAGE_FILE:
                    if (resultCode == RESULT_OK && data != null)
                        onAddImageFile(getUris(data));
                    break;
                case REQUEST_TAKE_PHOTO:
                    if (resultCode == RESULT_OK) {
                        if (photoURI != null)
                            onAddImageFile(Arrays.asList(photoURI));
                    }
                    break;
                case REQUEST_ATTACHMENT:
                case REQUEST_RECORD_AUDIO:
                    if (resultCode == RESULT_OK && data != null)
                        onAddAttachment(getUris(data), false, 0, false);
                    break;
                case REQUEST_OPENPGP:
                    if (resultCode == RESULT_OK && data != null)
                        onPgp(data);
                    break;
                case REQUEST_CONTACT_GROUP:
                    if (resultCode == RESULT_OK && data != null)
                        onContactGroupSelected(data.getBundleExtra("args"));
                    break;
                case REQUEST_LINK:
                    if (resultCode == RESULT_OK && data != null)
                        onLinkSelected(data.getBundleExtra("args"));
                    break;
                case REQUEST_DISCARD:
                    if (resultCode == RESULT_OK)
                        onActionDiscardConfirmed();
                    break;
                case REQUEST_SEND:
                    Bundle args = data.getBundleExtra("args");
                    Bundle extras = new Bundle();
                    extras.putBoolean("archive", args.getBoolean("archive"));
                    if (resultCode == RESULT_OK)
                        onAction(R.id.action_send, extras, "send");
                    else if (resultCode == RESULT_FIRST_USER) {
                        extras.putBoolean("now", true);
                        onAction(R.id.action_send, extras, "sendnow");
                    }
                    break;
                case REQUEST_REMOVE_ATTACHMENTS:
                    if (resultCode == RESULT_OK)
                        onRemoveAttachments();
                    break;
            }
        } catch (Throwable ex) {
            Log.e(ex);
        }
    }

    private void onPickContact(int requestCode, Intent data) {
        Uri uri = data.getData();
        if (uri == null)
            return;

        Bundle args = new Bundle();
        args.putLong("id", working);
        args.putString("to", etTo.getText().toString().trim());
        args.putString("cc", etCc.getText().toString().trim());
        args.putString("bcc", etBcc.getText().toString().trim());
        args.putInt("requestCode", requestCode);
        args.putParcelable("uri", uri);

        new SimpleTask<EntityMessage>() {
            @Override
            protected EntityMessage onExecute(Context context, Bundle args) throws Throwable {
                long id = args.getLong("id");
                String to = args.getString("to");
                String cc = args.getString("cc");
                String bcc = args.getString("bcc");
                int requestCode = args.getInt("requestCode");
                Uri uri = args.getParcelable("uri");

                if (uri == null)
                    throw new FileNotFoundException();

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                boolean suggest_names = prefs.getBoolean("suggest_names", true);

                EntityMessage draft = null;
                DB db = DB.getInstance(context);

                try (Cursor cursor = context.getContentResolver().query(
                        uri,
                        new String[]{
                                ContactsContract.CommonDataKinds.Email.ADDRESS,
                                ContactsContract.Contacts.DISPLAY_NAME
                        },
                        null, null, null)) {
                    // https://issuetracker.google.com/issues/118400813
                    // https://developer.android.com/guide/topics/providers/content-provider-basics#DisplayResults
                    if (cursor != null && cursor.getCount() == 0)
                        throw new SecurityException("Could not retrieve selected contact");

                    if (cursor != null && cursor.moveToFirst()) {
                        int colEmail = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS);
                        int colName = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                        if (colEmail >= 0 && colName >= 0) {
                            String email = MessageHelper.sanitizeEmail(cursor.getString(colEmail));
                            String name = cursor.getString(colName);

                            args.putString("email", email);

                            try {
                                db.beginTransaction();

                                draft = db.message().getMessage(id);
                                if (draft == null)
                                    return null;

                                draft.to = MessageHelper.parseAddresses(context, to);
                                draft.cc = MessageHelper.parseAddresses(context, cc);
                                draft.bcc = MessageHelper.parseAddresses(context, bcc);

                                Address[] address = null;
                                if (requestCode == REQUEST_CONTACT_TO)
                                    address = draft.to;
                                else if (requestCode == REQUEST_CONTACT_CC)
                                    address = draft.cc;
                                else if (requestCode == REQUEST_CONTACT_BCC)
                                    address = draft.bcc;

                                List<Address> list = new ArrayList<>();
                                if (address != null)
                                    list.addAll(Arrays.asList(address));

                                list.add(new InternetAddress(email, suggest_names ? name : null, StandardCharsets.UTF_8.name()));

                                if (requestCode == REQUEST_CONTACT_TO)
                                    draft.to = list.toArray(new Address[0]);
                                else if (requestCode == REQUEST_CONTACT_CC)
                                    draft.cc = list.toArray(new Address[0]);
                                else if (requestCode == REQUEST_CONTACT_BCC)
                                    draft.bcc = list.toArray(new Address[0]);

                                db.message().updateMessage(draft);

                                db.setTransactionSuccessful();
                            } finally {
                                db.endTransaction();
                            }
                        }
                    }
                }

                return draft;
            }

            @Override
            protected void onExecuted(Bundle args, EntityMessage draft) {
                if (requestCode == REQUEST_CONTACT_TO)
                    selectIdentityForEmail(args.getString("email"));

                if (draft != null) {
                    etTo.setText(MessageHelper.formatAddressesCompose(draft.to));
                    etCc.setText(MessageHelper.formatAddressesCompose(draft.cc));
                    etBcc.setText(MessageHelper.formatAddressesCompose(draft.bcc));
                }

                // After showDraft/setFocus
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (requestCode == REQUEST_CONTACT_TO)
                                etTo.setSelection(etTo.length());
                            else if (requestCode == REQUEST_CONTACT_CC)
                                etCc.setSelection(etCc.length());
                            else if (requestCode == REQUEST_CONTACT_BCC)
                                etBcc.setSelection(etBcc.length());
                        } catch (Throwable ex) {
                            Log.e(ex);
                        }
                    }
                });
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                if (ex instanceof SecurityException)
                    try {
                        pickRequest = requestCode;
                        pickUri = uri;
                        String permission = Manifest.permission.READ_CONTACTS;
                        requestPermissions(new String[]{permission}, REQUEST_PERMISSIONS);
                    } catch (Throwable ex1) {
                        Log.unexpectedError(getParentFragmentManager(), ex1);
                    }
                else
                    Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.setExecutor(executor).execute(this, args, "compose:picked");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (pickUri == null)
            return;
        for (int i = 0; i < permissions.length; i++)
            if (Manifest.permission.READ_CONTACTS.equals(permissions[i]))
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    onPickContact(pickRequest, new Intent().setData(pickUri));
    }

    private void onAddImage(boolean photo) {
        Context context = getContext();
        PackageManager pm = context.getPackageManager();
        if (photo) {
            // https://developer.android.com/training/camera/photobasics
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(pm) == null) { // action whitelisted
                Snackbar snackbar = Snackbar.make(view, getString(R.string.title_no_camera), Snackbar.LENGTH_LONG)
                        .setGestureInsetBottomIgnored(true);
                snackbar.setAction(R.string.title_fix, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Helper.viewFAQ(v.getContext(), 158);
                    }
                });
                snackbar.show();
            } else {
                File dir = new File(context.getFilesDir(), "photo");
                if (!dir.exists())
                    dir.mkdir();
                File file = new File(dir, working + ".jpg");
                try {
                    photoURI = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, file);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(intent, REQUEST_TAKE_PHOTO);
                } catch (Throwable ex) {
                    // java.lang.IllegalArgumentException: Failed to resolve canonical path for ...
                    Helper.reportNoViewer(context, intent, ex);
                }
            }
        } else {
            // https://developer.android.com/reference/android/provider/MediaStore#ACTION_PICK_IMAGES
            // Android 12: cmd device_config put storage_native_boot picker_intent_enabled true
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean photo_picker = prefs.getBoolean("photo_picker", true);
            Intent picker = new Intent(MediaStore.ACTION_PICK_IMAGES);
            picker.setType("image/*");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    photo_picker && picker.resolveActivity(pm) != null) {
                Log.i("Using photo picker");
                picker.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, MediaStore.getPickImagesMaxLimit());
                startActivityForResult(picker, REQUEST_IMAGE_FILE);
            } else {
                Log.i("Using file picker");
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                if (intent.resolveActivity(pm) == null) // GET_CONTENT whitelisted
                    noStorageAccessFramework();
                else
                    startActivityForResult(Helper.getChooser(context, intent), REQUEST_IMAGE_FILE);
            }
        }
    }

    private void onAddImageFile(List<Uri> uri) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean add_inline = prefs.getBoolean("add_inline", true);
        boolean resize_images = prefs.getBoolean("resize_images", true);
        boolean privacy_images = prefs.getBoolean("privacy_images", false);
        int resize = prefs.getInt("resize", FragmentCompose.REDUCED_IMAGE_SIZE);
        onAddAttachment(uri, add_inline, resize_images ? resize : 0, privacy_images);
    }

    private void onAddAttachment(List<Uri> uris, boolean image, int resize, boolean privacy) {
        etBody.clearComposingText();

        Bundle args = new Bundle();
        args.putLong("id", working);
        args.putParcelableArrayList("uris", new ArrayList<>(uris));
        args.putBoolean("image", image);
        args.putInt("resize", resize);
        args.putInt("zoom", zoom);
        args.putBoolean("privacy", privacy);
        args.putCharSequence("body", etBody.getText());
        args.putInt("start", etBody.getSelectionStart());

        new SimpleTask<Spanned>() {
            @Override
            protected Spanned onExecute(Context context, Bundle args) throws IOException {
                final long id = args.getLong("id");
                List<Uri> uris = args.getParcelableArrayList("uris");
                boolean image = args.getBoolean("image");
                int resize = args.getInt("resize");
                int zoom = args.getInt("zoom");
                boolean privacy = args.getBoolean("privacy");
                CharSequence body = args.getCharSequence("body");
                int start = args.getInt("start");

                SpannableStringBuilder s = new SpannableStringBuilderEx(body);
                if (start < 0)
                    start = 0;
                if (start > s.length())
                    start = s.length();

                for (Uri uri : uris) {
                    EntityAttachment attachment = addAttachment(context, id, uri, image, resize, privacy);
                    if (attachment == null)
                        continue;
                    if (!image)
                        continue;

                    File file = attachment.getFile(context);
                    Uri cid = Uri.parse("cid:" + BuildConfig.APPLICATION_ID + "." + attachment.id);

                    Drawable d;
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            ImageDecoder.Source source = ImageDecoder.createSource(file.getAbsoluteFile());
                            d = ImageDecoder.decodeDrawable(source);
                        } else
                            d = Drawable.createFromPath(file.getAbsolutePath());
                    } catch (Throwable ex) {
                        Log.w(ex);
                        d = Drawable.createFromPath(file.getAbsolutePath());
                    }

                    if (d == null) {
                        int px = Helper.dp2pixels(context, (zoom + 1) * 24);
                        d = ContextCompat.getDrawable(context, R.drawable.twotone_broken_image_24);
                        d.setBounds(0, 0, px, px);
                    }

                    s.insert(start, "\n\uFFFC\n"); // Object replacement character
                    ImageSpan is = new ImageSpan(context, cid);
                    s.setSpan(is, start + 1, start + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    start += 3;
                }

                if (!image)
                    return null;

                args.putInt("start", start);

                DB db = DB.getInstance(context);
                db.message().setMessagePlainOnly(id, 0);

                String html = HtmlHelper.toHtml(s, context);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                boolean write_below = prefs.getBoolean("write_below", false);

                EntityMessage draft = db.message().getMessage(id);
                if (draft != null) {
                    File file = draft.getFile(context);
                    Elements ref = JsoupEx.parse(file).select("div[fairemail=reference]");

                    Document doc = JsoupEx.parse(html);

                    for (Element e : ref)
                        if (write_below && draft.wasforwardedfrom == null)
                            doc.body().prependChild(e);
                        else
                            doc.body().appendChild(e);

                    EntityIdentity identity = db.identity().getIdentity(draft.identity);
                    addSignature(context, doc, draft, identity);

                    Helper.writeText(file, doc.html());
                }

                return HtmlHelper.fromHtml(html, new HtmlHelper.ImageGetterEx() {
                    @Override
                    public Drawable getDrawable(Element element) {
                        return ImageHelper.decodeImage(context,
                                id, element, true, zoom, 1.0f, etBody);
                    }
                }, null, getContext());
            }

            @Override
            protected void onExecuted(Bundle args, final Spanned body) {
                if (body == null)
                    return;

                etBody.setText(body);

                int start = args.getInt("start");
                if (start <= body.length())
                    etBody.setSelection(start);
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                handleException(ex);
            }
        }.setExecutor(executor).execute(this, args, "compose:attachment:add");
    }

    void onSharedAttachments(ArrayList<Uri> uris) {
        Bundle args = new Bundle();
        args.putLong("id", working);
        args.putParcelableArrayList("uris", uris);

        new SimpleTask<ArrayList<Uri>>() {
            @Override
            protected ArrayList<Uri> onExecute(Context context, Bundle args) throws Throwable {
                long id = args.getLong("id");
                List<Uri> uris = args.getParcelableArrayList("uris");

                ArrayList<Uri> images = new ArrayList<>();
                for (Uri uri : uris)
                    try {
                        UriInfo info = getInfo(uri, context);
                        if (info.isImage())
                            images.add(uri);
                        else
                            addAttachment(context, id, uri, false, 0, false);
                    } catch (IOException ex) {
                        Log.e(ex);
                    }

                return images;
            }

            @Override
            protected void onExecuted(Bundle args, ArrayList<Uri> images) {
                if (images.size() == 0)
                    return;

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                boolean image_dialog = prefs.getBoolean("image_dialog", true);

                if (image_dialog) {
                    Helper.hideKeyboard(view);

                    Bundle aargs = new Bundle();
                    aargs.putInt("title", android.R.string.ok);
                    aargs.putParcelableArrayList("images", images);

                    FragmentDialogAddImage fragment = new FragmentDialogAddImage();
                    fragment.setArguments(aargs);
                    fragment.setTargetFragment(FragmentCompose.this, REQUEST_SHARED);
                    fragment.show(getParentFragmentManager(), "compose:shared");
                } else
                    onAddImageFile(images);
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                handleException(ex);
            }
        }.execute(this, args, "compose:shared");
    }

    private List<Uri> getUris(Intent data) {
        List<Uri> result = new ArrayList<>();

        ClipData clipData = data.getClipData();
        if (clipData == null) {
            Uri uri = data.getData();
            if (uri != null)
                result.add(uri);
        } else {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item item = clipData.getItemAt(i);
                Uri uri = item.getUri();
                if (uri != null)
                    result.add(uri);
            }
        }

        // media-uri-list=[content://media/external_primary/images/media/nnn] (ArrayList)
        // media-file-list=[/storage/emulated/0/Pictures/...]
        // (ArrayList) media-id-list=[nnn] (ArrayList)
        if (result.size() == 0 && data.hasExtra("media-uri-list"))
            try {
                List<Uri> uris = data.getParcelableArrayListExtra("media-uri-list");
                result.addAll(uris);
            } catch (Throwable ex) {
                Log.e(ex);
            }

        return result;
    }

    private void onPgp(Intent data) {
        final Bundle args = new Bundle();
        args.putParcelable("data", data);

        new SimpleTask<Object>() {
            @Override
            protected void onPreExecute(Bundle args) {
                setBusy(true);
            }

            @Override
            protected void onPostExecute(Bundle args) {
                setBusy(false);
            }

            @Override
            protected Object onExecute(Context context, Bundle args) throws Throwable {
                // Get arguments
                Intent data = args.getParcelable("data");
                Bundle largs = data.getBundleExtra(BuildConfig.APPLICATION_ID);
                long id = largs.getLong("id", -1);
                String session = largs.getString("session");

                DB db = DB.getInstance(context);

                // Get data
                EntityMessage draft = db.message().getMessage(id);
                if (draft == null)
                    throw new MessageRemovedException("PGP");
                if (draft.identity == null)
                    throw new IllegalArgumentException(context.getString(R.string.title_from_missing));
                EntityIdentity identity = db.identity().getIdentity(draft.identity);
                if (identity == null)
                    throw new IllegalArgumentException(context.getString(R.string.title_from_missing));

                // Create files
                File tmp = new File(context.getFilesDir(), "encryption");
                if (!tmp.exists())
                    tmp.mkdir();
                File input = new File(tmp, draft.id + "_" + session + ".pgp_input");
                File output = new File(tmp, draft.id + "_" + session + ".pgp_output");

                // Serializing messages is NOT reproducible
                if ((EntityMessage.PGP_SIGNONLY.equals(draft.ui_encrypt) &&
                        OpenPgpApi.ACTION_GET_SIGN_KEY_ID.equals(data.getAction())) ||
                        (EntityMessage.PGP_SIGNENCRYPT.equals(draft.ui_encrypt) &&
                                OpenPgpApi.ACTION_GET_KEY_IDS.equals(data.getAction()))) {
                    // Get/clean attachments
                    List<EntityAttachment> attachments = db.attachment().getAttachments(draft.id);
                    for (EntityAttachment attachment : new ArrayList<>(attachments))
                        if (attachment.isEncryption()) {
                            db.attachment().deleteAttachment(attachment.id);
                            attachments.remove(attachment);
                        }

                    // Build message
                    Properties props = MessageHelper.getSessionProperties(true);
                    Session isession = Session.getInstance(props, null);
                    MimeMessage imessage = new MimeMessage(isession);
                    MessageHelper.build(context, draft, attachments, identity, true, imessage);

                    if (OpenPgpApi.ACTION_GET_SIGN_KEY_ID.equals(data.getAction())) {
                        // Serialize content
                        imessage.saveChanges();
                        BodyPart bpContent = new MimeBodyPart() {
                            @Override
                            public void setContent(Object content, String type) throws MessagingException {
                                super.setContent(content, type);

                                // https://javaee.github.io/javamail/FAQ#howencode
                                updateHeaders();
                                if (content instanceof Multipart) {
                                    try {
                                        MessageHelper.overrideContentTransferEncoding((Multipart) content);
                                    } catch (IOException ex) {
                                        Log.e(ex);
                                    }
                                } else
                                    setHeader("Content-Transfer-Encoding", "base64");
                            }
                        };
                        bpContent.setContent(imessage.getContent(), imessage.getContentType());

                        try (OutputStream out = new FileOutputStream(input)) {
                            bpContent.writeTo(out);
                        }
                    } else {
                        // Serialize message
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                        boolean encrypt_subject = prefs.getBoolean("encrypt_subject", false);
                        if (encrypt_subject) {
                            // https://tools.ietf.org/id/draft-autocrypt-lamps-protected-headers-01.html
                            imessage.saveChanges();
                            BodyPart bpContent = new MimeBodyPart() {
                                @Override
                                public void setContent(Object content, String type) throws MessagingException {
                                    super.setContent(content, type);

                                    updateHeaders();

                                    ContentType ct = new ContentType(type);
                                    ct.setParameter("protected-headers", "v1");
                                    setHeader("Content-Type", ct.toString());
                                    String subject = (draft.subject == null ? "" : draft.subject);
                                    try {
                                        setHeader("Subject", MimeUtility.encodeWord(subject));
                                    } catch (UnsupportedEncodingException ex) {
                                        Log.e(ex);
                                        setHeader("Subject", subject);
                                    }
                                }
                            };

                            bpContent.setContent(imessage.getContent(), imessage.getContentType());

                            try (OutputStream out = new FileOutputStream(input)) {
                                bpContent.writeTo(out);
                            }
                        } else
                            try (OutputStream out = new FileOutputStream(input)) {
                                imessage.writeTo(out);
                            }
                    }
                }

                Intent result;
                if (OpenPgpApi.ACTION_GET_SIGN_KEY_ID.equals(data.getAction()) && identity.sign_key != null) {
                    // Short circuit
                    result = data;
                    result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
                    result.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, identity.sign_key);
                } else {
                    // Call OpenPGP
                    result = PgpHelper.execute(context, data, new FileInputStream(input), new FileOutputStream(output));
                }

                // Process result
                try {
                    int resultCode = result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
                    switch (resultCode) {
                        case OpenPgpApi.RESULT_CODE_SUCCESS:
                            // Attach key, signed/encrypted data
                            if (OpenPgpApi.ACTION_GET_KEY.equals(data.getAction()) ||
                                    OpenPgpApi.ACTION_DETACHED_SIGN.equals(data.getAction()) ||
                                    OpenPgpApi.ACTION_SIGN_AND_ENCRYPT.equals(data.getAction()))
                                try {
                                    db.beginTransaction();

                                    String name;
                                    ContentType ct = new ContentType("application/octet-stream");
                                    int encryption;
                                    if (OpenPgpApi.ACTION_GET_KEY.equals(data.getAction())) {
                                        name = "keydata.asc";
                                        encryption = EntityAttachment.PGP_KEY;
                                    } else if (OpenPgpApi.ACTION_DETACHED_SIGN.equals(data.getAction())) {
                                        name = "signature.asc";
                                        encryption = EntityAttachment.PGP_SIGNATURE;
                                        String micalg = result.getStringExtra(OpenPgpApi.RESULT_SIGNATURE_MICALG);
                                        if (TextUtils.isEmpty(micalg))
                                            throw new IllegalArgumentException("micalg missing");
                                        ct = new ContentType("application/pgp-signature");
                                        ct.setParameter("micalg", micalg);
                                    } else if (OpenPgpApi.ACTION_SIGN_AND_ENCRYPT.equals(data.getAction())) {
                                        name = "encrypted.asc";
                                        encryption = EntityAttachment.PGP_MESSAGE;
                                    } else
                                        throw new IllegalStateException(data.getAction());

                                    EntityAttachment attachment = new EntityAttachment();
                                    attachment.message = draft.id;
                                    attachment.sequence = db.attachment().getAttachmentSequence(draft.id) + 1;
                                    attachment.name = name;
                                    attachment.type = ct.toString();
                                    attachment.disposition = Part.INLINE;
                                    attachment.encryption = encryption;
                                    attachment.id = db.attachment().insertAttachment(attachment);

                                    File file = attachment.getFile(context);

                                    if (OpenPgpApi.ACTION_DETACHED_SIGN.equals(data.getAction())) {
                                        byte[] bytes = result.getByteArrayExtra(OpenPgpApi.RESULT_DETACHED_SIGNATURE);
                                        Log.i("Writing " + file + " size=" + bytes.length);
                                        try (OutputStream out = new FileOutputStream(file)) {
                                            out.write(bytes);
                                        }
                                        db.attachment().setDownloaded(attachment.id, (long) bytes.length);
                                    } else {
                                        Log.i("Writing " + file + " size=" + output.length());
                                        Helper.copy(output, file);
                                        db.attachment().setDownloaded(attachment.id, file.length());
                                    }

                                    db.setTransactionSuccessful();
                                } finally {
                                    db.endTransaction();
                                }

                            // Sign-only: [get sign key id], get key, detached sign
                            // Sign/encrypt: get key ids, [get sign key id], get key, sign and encrypt

                            if (OpenPgpApi.ACTION_GET_KEY_IDS.equals(data.getAction())) {
                                // Sign/encrypt
                                pgpKeyIds = result.getLongArrayExtra(OpenPgpApi.EXTRA_KEY_IDS);
                                Log.i("Keys=" + pgpKeyIds.length);
                                if (pgpKeyIds.length == 0) // One key can be for multiple users
                                    throw new IllegalArgumentException(context.getString(R.string.title_key_missing,
                                            TextUtils.join(", ", pgpUserIds)));

                                if (identity.sign_key != null) {
                                    pgpSignKeyId = identity.sign_key;

                                    // Get public key
                                    Intent intent = new Intent(OpenPgpApi.ACTION_GET_KEY);
                                    intent.putExtra(OpenPgpApi.EXTRA_KEY_ID, pgpSignKeyId);
                                    intent.putExtra(OpenPgpApi.EXTRA_MINIMIZE, true);
                                    intent.putExtra(OpenPgpApi.EXTRA_MINIMIZE_USER_ID, identity.email);
                                    intent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);
                                    intent.putExtra(BuildConfig.APPLICATION_ID, largs);
                                    return intent;
                                } else {
                                    // Get sign key
                                    Intent intent = new Intent(OpenPgpApi.ACTION_GET_SIGN_KEY_ID);
                                    intent.putExtra(BuildConfig.APPLICATION_ID, largs);
                                    return intent;
                                }
                            } else if (OpenPgpApi.ACTION_GET_SIGN_KEY_ID.equals(data.getAction())) {
                                pgpSignKeyId = result.getLongExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, -1);
                                if (pgpSignKeyId == 0)
                                    throw new IllegalArgumentException(context.getString(R.string.title_no_sign_key));
                                db.identity().setIdentitySignKey(identity.id, pgpSignKeyId);

                                // Get public key
                                Intent intent = new Intent(OpenPgpApi.ACTION_GET_KEY);
                                intent.putExtra(OpenPgpApi.EXTRA_KEY_ID, pgpSignKeyId);
                                intent.putExtra(OpenPgpApi.EXTRA_MINIMIZE, true);
                                intent.putExtra(OpenPgpApi.EXTRA_MINIMIZE_USER_ID, identity.email);
                                intent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);
                                intent.putExtra(BuildConfig.APPLICATION_ID, largs);
                                return intent;
                            } else if (OpenPgpApi.ACTION_GET_KEY.equals(data.getAction())) {
                                if (EntityMessage.PGP_SIGNONLY.equals(draft.ui_encrypt)) {
                                    // Get signature
                                    Intent intent = new Intent(OpenPgpApi.ACTION_DETACHED_SIGN);
                                    intent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, pgpSignKeyId);
                                    intent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);
                                    intent.putExtra(BuildConfig.APPLICATION_ID, largs);
                                    return intent;
                                } else if (EntityMessage.PGP_SIGNENCRYPT.equals(draft.ui_encrypt)) {
                                    // Encrypt message
                                    Intent intent = new Intent(OpenPgpApi.ACTION_SIGN_AND_ENCRYPT);
                                    intent.putExtra(OpenPgpApi.EXTRA_KEY_IDS, pgpKeyIds);
                                    intent.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, pgpSignKeyId);
                                    intent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);
                                    intent.putExtra(BuildConfig.APPLICATION_ID, largs);
                                    return intent;
                                } else
                                    throw new IllegalArgumentException("Invalid encrypt=" + draft.ui_encrypt);
                            } else if (OpenPgpApi.ACTION_DETACHED_SIGN.equals(data.getAction())) {
                                EntityAttachment attachment = new EntityAttachment();
                                attachment.message = draft.id;
                                attachment.sequence = db.attachment().getAttachmentSequence(draft.id) + 1;
                                attachment.name = "content.asc";
                                attachment.type = "text/plain";
                                attachment.disposition = Part.INLINE;
                                attachment.encryption = EntityAttachment.PGP_CONTENT;
                                attachment.id = db.attachment().insertAttachment(attachment);

                                File file = attachment.getFile(context);
                                input.renameTo(file);

                                db.attachment().setDownloaded(attachment.id, file.length());

                                // send message
                                args.putInt("action", largs.getInt("action"));
                                args.putBundle("extras", largs.getBundle("extras"));
                                return null;
                            } else if (OpenPgpApi.ACTION_SIGN_AND_ENCRYPT.equals(data.getAction())) {
                                input.delete();

                                // send message
                                args.putInt("action", largs.getInt("action"));
                                args.putBundle("extras", largs.getBundle("extras"));
                                return null;
                            } else
                                throw new IllegalStateException("Unknown action=" + data.getAction());

                        case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED:
                            args.putBoolean("interactive", largs.getBoolean("interactive"));
                            return result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);

                        case OpenPgpApi.RESULT_CODE_ERROR:
                            input.delete();
                            db.identity().setIdentitySignKey(identity.id, null);
                            OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                            if (error != null &&
                                    error.getErrorId() == 0 && error.getMessage() == null)
                                error.setMessage("General error");
                            throw new IllegalArgumentException(
                                    "OpenPgp" +
                                            " error " + (error == null ? "?" : error.getErrorId()) +
                                            ": " + (error == null ? "?" : error.getMessage()));

                        default:
                            throw new IllegalStateException("OpenPgp unknown result code=" + resultCode);
                    }
                } finally {
                    output.delete();
                }
            }

            @Override
            protected void onExecuted(Bundle args, Object result) {
                Log.i("Result= " + result);
                if (result == null) {
                    int action = args.getInt("action");
                    Bundle extras = args.getBundle("extras");
                    extras.putBoolean("encrypted", true);
                    onAction(action, extras, "pgp");
                } else if (result instanceof Intent) {
                    Intent intent = (Intent) result;
                    onPgp(intent);
                } else if (result instanceof PendingIntent)
                    if (args.getBoolean("interactive"))
                        try {
                            ToastEx.makeText(getContext(), R.string.title_user_interaction, Toast.LENGTH_SHORT).show();
                            PendingIntent pi = (PendingIntent) result;
                            startIntentSenderForResult(
                                    pi.getIntentSender(),
                                    REQUEST_OPENPGP,
                                    null, 0, 0, 0, null);
                        } catch (IntentSender.SendIntentException ex) {
                            Log.e(ex);
                            Log.unexpectedError(getParentFragmentManager(), ex);
                        }
                    else {
                        if (BuildConfig.DEBUG)
                            ToastEx.makeText(getContext(), "Non interactive", Toast.LENGTH_SHORT).show();
                    }
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                if (ex instanceof IllegalArgumentException
                        || ex instanceof GeneralSecurityException /* InvalidKeyException */) {
                    Log.i(ex);
                    Snackbar.make(view, ex.getMessage(), Snackbar.LENGTH_LONG)
                            .setGestureInsetBottomIgnored(true).show();
                } else if (ex instanceof OperationCanceledException) {
                    Snackbar snackbar = Snackbar.make(view, R.string.title_no_openpgp, Snackbar.LENGTH_INDEFINITE)
                            .setGestureInsetBottomIgnored(true);
                    snackbar.setAction(R.string.title_fix, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            snackbar.dismiss();
                            Helper.viewFAQ(v.getContext(), 12);
                        }
                    });
                    snackbar.show();
                } else
                    Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.setExecutor(executor).execute(this, args, "compose:pgp");
    }

    private void onSmime(Bundle args, final int action, final Bundle extras) {
        new SimpleTask<Void>() {
            @Override
            protected void onPreExecute(Bundle args) {
                setBusy(true);
            }

            @Override
            protected void onPostExecute(Bundle args) {
                setBusy(false);
            }

            @Override
            protected Void onExecute(Context context, Bundle args) throws Throwable {
                long id = args.getLong("id");
                int type = args.getInt("type");
                String alias = args.getString("alias");

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                boolean check_certificate = prefs.getBoolean("check_certificate", true);

                File tmp = new File(context.getFilesDir(), "encryption");
                if (!tmp.exists())
                    tmp.mkdir();

                DB db = DB.getInstance(context);

                // Get data
                EntityMessage draft = db.message().getMessage(id);
                if (draft == null)
                    throw new MessageRemovedException("S/MIME");
                EntityIdentity identity = db.identity().getIdentity(draft.identity);
                if (identity == null)
                    throw new IllegalArgumentException(context.getString(R.string.title_from_missing));

                // Get/clean attachments
                List<EntityAttachment> attachments = db.attachment().getAttachments(id);
                for (EntityAttachment attachment : new ArrayList<>(attachments))
                    if (attachment.encryption != null) {
                        db.attachment().deleteAttachment(attachment.id);
                        attachments.remove(attachment);
                    }

                // Build message to sign
                //   openssl smime -verify <xxx.eml
                Properties props = MessageHelper.getSessionProperties(true);
                Session isession = Session.getInstance(props, null);
                MimeMessage imessage = new MimeMessage(isession);
                MessageHelper.build(context, draft, attachments, identity, true, imessage);
                imessage.saveChanges();
                BodyPart bpContent = new MimeBodyPart() {
                    @Override
                    public void setContent(Object content, String type) throws MessagingException {
                        super.setContent(content, type);

                        // https://javaee.github.io/javamail/FAQ#howencode
                        updateHeaders();
                        if (content instanceof Multipart) {
                            try {
                                MessageHelper.overrideContentTransferEncoding((Multipart) content);
                            } catch (IOException ex) {
                                Log.e(ex);
                            }
                        } else
                            setHeader("Content-Transfer-Encoding", "base64");
                    }
                };
                bpContent.setContent(imessage.getContent(), imessage.getContentType());

                if (alias == null)
                    throw new IllegalArgumentException("Key alias missing");

                // Get private key
                PrivateKey privkey = KeyChain.getPrivateKey(context, alias);
                if (privkey == null)
                    throw new IllegalArgumentException("Private key missing");

                // Get public key
                X509Certificate[] chain = KeyChain.getCertificateChain(context, alias);
                if (chain == null || chain.length == 0)
                    throw new IllegalArgumentException("Certificate missing");

                if (check_certificate) {
                    // Check public key validity
                    try {
                        chain[0].checkValidity();
                        // TODO: check digitalSignature/nonRepudiation key usage
                        // https://datatracker.ietf.org/doc/html/rfc3850#section-4.4.2
                    } catch (CertificateException ex) {
                        String msg = ex.getMessage();
                        throw new IllegalArgumentException(
                                TextUtils.isEmpty(msg) ? Log.formatThrowable(ex) : msg);
                    }

                    // Check public key email
                    boolean known = false;
                    List<String> emails = EntityCertificate.getEmailAddresses(chain[0]);
                    for (String email : emails)
                        if (email.equalsIgnoreCase(identity.email)) {
                            known = true;
                            break;
                        }

                    if (!known && emails.size() > 0) {
                        String message = identity.email + " (" + TextUtils.join(", ", emails) + ")";
                        throw new IllegalArgumentException(
                                context.getString(R.string.title_certificate_missing, message),
                                new CertificateException());
                    }
                }

                // Store selected alias
                db.identity().setIdentitySignKeyAlias(identity.id, alias);

                // Build content
                if (EntityMessage.SMIME_SIGNONLY.equals(type)) {
                    EntityAttachment cattachment = new EntityAttachment();
                    cattachment.message = draft.id;
                    cattachment.sequence = db.attachment().getAttachmentSequence(draft.id) + 1;
                    cattachment.name = "content.asc";
                    cattachment.type = "text/plain";
                    cattachment.disposition = Part.INLINE;
                    cattachment.encryption = EntityAttachment.SMIME_CONTENT;
                    cattachment.id = db.attachment().insertAttachment(cattachment);

                    File content = cattachment.getFile(context);
                    try (OutputStream os = new FileOutputStream(content)) {
                        bpContent.writeTo(os);
                    }

                    db.attachment().setDownloaded(cattachment.id, content.length());
                }

                // Sign
                Store store = new JcaCertStore(Arrays.asList(chain));
                CMSSignedDataGenerator cmsGenerator = new CMSSignedDataGenerator();
                cmsGenerator.addCertificates(store);

                String signAlgorithm = prefs.getString("sign_algo_smime", "SHA-256");

                String algorithm = privkey.getAlgorithm();
                if (TextUtils.isEmpty(algorithm) || "RSA".equals(algorithm))
                    Log.i("Private key algorithm=" + algorithm);
                else
                    Log.e("Private key algorithm=" + algorithm);

                if (TextUtils.isEmpty(algorithm))
                    algorithm = "RSA";
                else if ("EC".equals(algorithm))
                    algorithm = "ECDSA";

                algorithm = signAlgorithm.replace("-", "") + "with" + algorithm;
                Log.i("Sign algorithm=" + algorithm);

                ContentSigner contentSigner = new JcaContentSignerBuilder(algorithm)
                        .build(privkey);
                DigestCalculatorProvider digestCalculator = new JcaDigestCalculatorProviderBuilder()
                        .build();
                SignerInfoGenerator signerInfoGenerator = new JcaSignerInfoGeneratorBuilder(digestCalculator)
                        .build(contentSigner, chain[0]);
                cmsGenerator.addSignerInfoGenerator(signerInfoGenerator);

                File sinput = new File(tmp, draft.id + ".smime_sign");
                try (FileOutputStream fos = new FileOutputStream(sinput)) {
                    bpContent.writeTo(fos);
                }

                CMSTypedData cmsData = new CMSProcessableFile(sinput);
                CMSSignedData cmsSignedData = cmsGenerator.generate(cmsData);
                byte[] signedMessage = cmsSignedData.getEncoded();

                sinput.delete();

                // Build signature
                if (EntityMessage.SMIME_SIGNONLY.equals(type)) {
                    ContentType ct = new ContentType("application/pkcs7-signature");
                    ct.setParameter("micalg", signAlgorithm.toLowerCase(Locale.ROOT));

                    EntityAttachment sattachment = new EntityAttachment();
                    sattachment.message = draft.id;
                    sattachment.sequence = db.attachment().getAttachmentSequence(draft.id) + 1;
                    sattachment.name = "smime.p7s";
                    sattachment.type = ct.toString();
                    sattachment.disposition = Part.INLINE;
                    sattachment.encryption = EntityAttachment.SMIME_SIGNATURE;
                    sattachment.id = db.attachment().insertAttachment(sattachment);

                    File file = sattachment.getFile(context);
                    try (OutputStream os = new FileOutputStream(file)) {
                        os.write(signedMessage);
                    }

                    db.attachment().setDownloaded(sattachment.id, file.length());

                    return null;
                }

                List<Address> addresses = new ArrayList<>();
                if (draft.to != null)
                    addresses.addAll(Arrays.asList(draft.to));
                if (draft.cc != null)
                    addresses.addAll(Arrays.asList(draft.cc));
                if (draft.bcc != null)
                    addresses.addAll(Arrays.asList(draft.bcc));

                List<X509Certificate> certs = new ArrayList<>();

                boolean own = true;
                for (Address address : addresses) {
                    boolean found = false;
                    Throwable cex = null;
                    String email = ((InternetAddress) address).getAddress();
                    List<EntityCertificate> acertificates = db.certificate().getCertificateByEmail(email);
                    if (acertificates != null)
                        for (EntityCertificate acertificate : acertificates) {
                            X509Certificate cert = acertificate.getCertificate();
                            try {
                                cert.checkValidity();
                                certs.add(cert);
                                found = true;
                                if (cert.equals(chain[0]))
                                    own = false;
                            } catch (CertificateException ex) {
                                Log.w(ex);
                                cex = ex;
                            }
                        }

                    if (!found)
                        if (cex == null)
                            throw new IllegalArgumentException(
                                    context.getString(R.string.title_certificate_missing, email));
                        else
                            throw new IllegalArgumentException(
                                    context.getString(R.string.title_certificate_invalid, email), cex);
                }

                // Allow sender to decrypt own message
                if (own)
                    certs.add(chain[0]);

                // Build signature
                BodyPart bpSignature = new MimeBodyPart();
                bpSignature.setFileName("smime.p7s");
                bpSignature.setDataHandler(new DataHandler(new ByteArrayDataSource(signedMessage, "application/pkcs7-signature")));
                bpSignature.setDisposition(Part.INLINE);

                // Build message
                ContentType ct = new ContentType("multipart/signed");
                ct.setParameter("micalg", signAlgorithm.toLowerCase(Locale.ROOT));
                ct.setParameter("protocol", "application/pkcs7-signature");
                ct.setParameter("smime-type", "signed-data");
                String ctx = ct.toString();
                int slash = ctx.indexOf("/");
                Multipart multipart = new MimeMultipart(ctx.substring(slash + 1));
                multipart.addBodyPart(bpContent);
                multipart.addBodyPart(bpSignature);
                imessage.setContent(multipart);
                imessage.saveChanges();

                // Encrypt
                CMSEnvelopedDataGenerator cmsEnvelopedDataGenerator = new CMSEnvelopedDataGenerator();
                if ("EC".equals(privkey.getAlgorithm())) {
                    // https://datatracker.ietf.org/doc/html/draft-ietf-smime-3278bis
                    JceKeyAgreeRecipientInfoGenerator gen = new JceKeyAgreeRecipientInfoGenerator(
                            CMSAlgorithm.ECCDH_SHA256KDF,
                            privkey,
                            chain[0].getPublicKey(),
                            CMSAlgorithm.AES128_WRAP);
                    for (X509Certificate cert : certs)
                        gen.addRecipient(cert);
                    cmsEnvelopedDataGenerator.addRecipientInfoGenerator(gen);
                    // https://security.stackexchange.com/a/53960
                    // throw new IllegalArgumentException("ECDSA cannot be used for encryption");
                } else {
                    for (X509Certificate cert : certs) {
                        RecipientInfoGenerator gen = new JceKeyTransRecipientInfoGenerator(cert);
                        cmsEnvelopedDataGenerator.addRecipientInfoGenerator(gen);
                    }
                }

                File einput = new File(tmp, draft.id + ".smime_encrypt");
                try (FileOutputStream fos = new FileOutputStream(einput)) {
                    imessage.writeTo(fos);
                }
                CMSTypedData msg = new CMSProcessableFile(einput);

                ASN1ObjectIdentifier encryptionOID;
                String encryptAlgorithm = prefs.getString("encrypt_algo_smime", "AES-128");
                switch (encryptAlgorithm) {
                    case "AES-128":
                        encryptionOID = CMSAlgorithm.AES128_CBC;
                        break;
                    case "AES-192":
                        encryptionOID = CMSAlgorithm.AES192_CBC;
                        break;
                    case "AES-256":
                        encryptionOID = CMSAlgorithm.AES256_CBC;
                        break;
                    default:
                        encryptionOID = CMSAlgorithm.AES128_CBC;
                }
                Log.i("Encryption algorithm=" + encryptAlgorithm + " OID=" + encryptionOID);

                OutputEncryptor encryptor = new JceCMSContentEncryptorBuilder(encryptionOID)
                        .build();
                CMSEnvelopedData cmsEnvelopedData = cmsEnvelopedDataGenerator
                        .generate(msg, encryptor);

                EntityAttachment attachment = new EntityAttachment();
                attachment.message = draft.id;
                attachment.sequence = db.attachment().getAttachmentSequence(draft.id) + 1;
                attachment.name = "smime.p7m";
                attachment.type = "application/pkcs7-mime";
                attachment.disposition = Part.INLINE;
                attachment.encryption = EntityAttachment.SMIME_MESSAGE;
                attachment.id = db.attachment().insertAttachment(attachment);

                File encrypted = attachment.getFile(context);
                try (OutputStream os = new FileOutputStream(encrypted)) {
                    cmsEnvelopedData.toASN1Structure().encodeTo(os);
                }

                einput.delete();

                db.attachment().setDownloaded(attachment.id, encrypted.length());

                return null;
            }

            @Override
            protected void onExecuted(Bundle args, Void result) {
                extras.putBoolean("encrypted", true);
                onAction(action, extras, "smime");
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                if (ex instanceof IllegalArgumentException) {
                    Log.i(ex);
                    Snackbar snackbar = Snackbar.make(view, ex.getMessage(), Snackbar.LENGTH_INDEFINITE)
                            .setGestureInsetBottomIgnored(true);
                    Helper.setSnackbarLines(snackbar, 7);
                    snackbar.setAction(R.string.title_fix, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (ex.getCause() instanceof CertificateException)
                                v.getContext().startActivity(new Intent(v.getContext(), ActivitySetup.class)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        .putExtra("tab", "encryption"));
                            else {
                                PopupMenuLifecycle popupMenu = new PopupMenuLifecycle(getContext(), getViewLifecycleOwner(), vwAnchor);
                                popupMenu.getMenu().add(Menu.NONE, R.string.title_send_dialog, 1, R.string.title_send_dialog);
                                popupMenu.getMenu().add(Menu.NONE, R.string.title_advanced_manage_certificates, 2, R.string.title_advanced_manage_certificates);

                                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        int itemId = item.getItemId();
                                        if (itemId == R.string.title_send_dialog) {
                                            Helper.hideKeyboard(view);

                                            FragmentDialogSend fragment = new FragmentDialogSend();
                                            fragment.setArguments(args);
                                            fragment.setTargetFragment(FragmentCompose.this, REQUEST_SEND);
                                            fragment.show(getParentFragmentManager(), "compose:send");
                                            return true;
                                        } else if (itemId == R.string.title_advanced_manage_certificates) {
                                            startActivity(new Intent(getContext(), ActivitySetup.class)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                    .putExtra("tab", "encryption"));
                                            return true;
                                        }
                                        return false;
                                    }
                                });

                                popupMenu.show();
                            }
                        }
                    });
                    snackbar.show();
                } else {
                    boolean expected =
                            (ex instanceof OperatorCreationException &&
                                    ex.getCause() instanceof InvalidKeyException);
                    Log.unexpectedError(getParentFragmentManager(), ex, !expected);
                }
            }
        }.setExecutor(executor).execute(this, args, "compose:s/mime");
    }

    private void onContactGroupSelected(Bundle args) {
        final int target = args.getInt("target");
        if (target > 0)
            grpAddresses.setVisibility(View.VISIBLE);

        args.putString("to", etTo.getText().toString().trim());
        args.putString("cc", etCc.getText().toString().trim());
        args.putString("bcc", etBcc.getText().toString().trim());

        new SimpleTask<EntityMessage>() {
            @Override
            protected EntityMessage onExecute(Context context, Bundle args) throws Throwable {
                long id = args.getLong("id");
                int target = args.getInt("target");
                long group = args.getLong("group");
                String gname = args.getString("name");
                int type = args.getInt("type");
                String to = args.getString("to");
                String cc = args.getString("cc");
                String bcc = args.getString("bcc");

                EntityLog.log(context, "Selected group=" + group + "/" + gname);

                List<Address> selected = new ArrayList<>();

                if (group < 0) {
                    DB db = DB.getInstance(context);
                    List<EntityContact> contacts = db.contact().getContacts(gname);
                    if (contacts != null)
                        for (EntityContact contact : contacts) {
                            Address address = new InternetAddress(contact.email, contact.name, StandardCharsets.UTF_8.name());
                            selected.add(address);
                        }
                } else
                    try (Cursor cursor = context.getContentResolver().query(
                            ContactsContract.Data.CONTENT_URI,
                            new String[]{ContactsContract.Data.CONTACT_ID},
                            ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + "= ?" + " AND "
                                    + ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE + "='"
                                    + ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE + "'",
                            new String[]{String.valueOf(group)}, null)) {
                        while (cursor != null && cursor.moveToNext()) {
                            // https://developer.android.com/reference/android/provider/ContactsContract.CommonDataKinds.Email
                            try (Cursor contact = getContext().getContentResolver().query(
                                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                                    new String[]{
                                            ContactsContract.Contacts.DISPLAY_NAME,
                                            ContactsContract.CommonDataKinds.Email.DATA,
                                            ContactsContract.CommonDataKinds.Email.TYPE,
                                    },
                                    ContactsContract.Data.CONTACT_ID + " = ?",
                                    new String[]{cursor.getString(0)},
                                    null)) {
                                if (contact != null && contact.moveToNext()) {
                                    String name = contact.getString(0);
                                    String email = contact.getString(1);
                                    int etype = contact.getInt(2);
                                    Address address = new InternetAddress(email, name, StandardCharsets.UTF_8.name());
                                    EntityLog.log(context, "Selected group=" + group + ":" + type +
                                            " address=" + MessageHelper.formatAddresses(new Address[]{address}) + ":" + etype);
                                    if (type == 0 || etype == type)
                                        selected.add(address);
                                }
                            }
                        }
                    }

                EntityMessage draft;
                DB db = DB.getInstance(context);

                try {
                    db.beginTransaction();

                    draft = db.message().getMessage(id);
                    if (draft == null)
                        return null;

                    draft.to = MessageHelper.parseAddresses(context, to);
                    draft.cc = MessageHelper.parseAddresses(context, cc);
                    draft.bcc = MessageHelper.parseAddresses(context, bcc);

                    Address[] address = null;
                    if (target == 0)
                        address = draft.to;
                    else if (target == 1)
                        address = draft.cc;
                    else if (target == 2)
                        address = draft.bcc;

                    List<Address> list = new ArrayList<>();
                    if (address != null)
                        list.addAll(Arrays.asList(address));

                    list.addAll(selected);

                    if (target == 0)
                        draft.to = list.toArray(new Address[0]);
                    else if (target == 1)
                        draft.cc = list.toArray(new Address[0]);
                    else if (target == 2)
                        draft.bcc = list.toArray(new Address[0]);

                    db.message().updateMessage(draft);

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                return draft;
            }

            @Override
            protected void onExecuted(Bundle args, EntityMessage draft) {
                if (draft == null)
                    return;

                EditText edit;
                String text;

                if (target == 0) {
                    edit = etTo;
                    text = MessageHelper.formatAddressesCompose(draft.to);
                } else if (target == 1) {
                    edit = etCc;
                    text = MessageHelper.formatAddressesCompose(draft.cc);
                } else if (target == 2) {
                    edit = etBcc;
                    text = MessageHelper.formatAddressesCompose(draft.bcc);
                } else
                    return;

                edit.setText(text);
                edit.setSelection(text.length());
                edit.requestFocus();
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.setExecutor(executor).execute(this, args, "compose:picked");
    }

    private void onLinkSelected(Bundle args) {
        String link = args.getString("link");
        int start = args.getInt("start");
        int end = args.getInt("end");
        String title = args.getString("title");
        etBody.setSelection(start, end);
        StyleHelper.apply(R.id.menu_link, getViewLifecycleOwner(), null, etBody, link, title);
    }

    private void onActionDiscardConfirmed() {
        onAction(R.id.action_delete, "delete");
    }

    private void onRemoveAttachments() {
        Bundle args = new Bundle();
        args.putLong("id", working);

        new SimpleTask<Void>() {
            @Override
            protected Void onExecute(Context context, Bundle args) throws Throwable {
                long id = args.getLong("id");

                DB db = DB.getInstance(context);
                db.attachment().deleteAttachments(id);

                return null;
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.execute(FragmentCompose.this, args, "attachments:remove");
    }

    private void onExit() {
        if (state == State.LOADED) {
            state = State.NONE;
            if (!saved && isEmpty())
                onAction(R.id.action_delete, "empty");
            else {
                Bundle extras = new Bundle();
                extras.putBoolean("autosave", true);
                onAction(R.id.action_save, extras, "exit");
                finish();
            }
        } else
            finish();
    }

    private boolean isEmpty() {
        if (!etSubject.getText().toString().equals(subject))
            return false;

        if (!TextUtils.isEmpty(JsoupEx.parse(HtmlHelper.toHtml(etBody.getText(), getContext())).text().trim()))
            return false;

        if (rvAttachment.getAdapter().getItemCount() > 0)
            return false;

        return true;
    }

    private void onAction(int action, String reason) {
        onAction(action, new Bundle(), reason);
    }

    private void onAction(int action, @NonNull Bundle extras, String reason) {
        EntityIdentity identity = (EntityIdentity) spIdentity.getSelectedItem();

        View focus = view.findFocus();
        boolean ime = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            try {
                ime = view.getRootWindowInsets().isVisible(WindowInsetsCompat.Type.ime());
            } catch (Throwable ex) {
                Log.e(ex);
            }

        // Workaround underlines left by Android
        etBody.clearComposingText();

        Editable e = etBody.getText();
        boolean notext = e.toString().trim().isEmpty();

        Bundle args = new Bundle();
        args.putLong("id", working);
        args.putInt("action", action);
        args.putLong("account", identity == null ? -1 : identity.account);
        args.putLong("identity", identity == null ? -1 : identity.id);
        args.putString("extra", etExtra.getText().toString().trim());
        args.putString("to", etTo.getText().toString().trim());
        args.putString("cc", etCc.getText().toString().trim());
        args.putString("bcc", etBcc.getText().toString().trim());
        args.putString("subject", etSubject.getText().toString().trim());
        args.putCharSequence("loaded", (Spanned) etBody.getTag());
        args.putCharSequence("spanned", etBody.getText());
        args.putBoolean("signature", cbSignature.isChecked());
        args.putBoolean("empty", isEmpty());
        args.putBoolean("notext", notext);
        args.putBoolean("interactive", getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED));
        args.putInt("focus", focus == null ? -1 : focus.getId());
        if (focus instanceof EditText) {
            args.putInt("start", ((EditText) focus).getSelectionStart());
            args.putInt("end", ((EditText) focus).getSelectionEnd());
        }
        args.putBoolean("ime", ime);
        args.putBundle("extras", extras);

        Log.i("Run execute id=" + working + " reason=" + reason);
        actionLoader.execute(this, args, "compose:action:" + getActionName(action));
    }

    private static EntityAttachment addAttachment(
            Context context, long id, Uri uri, boolean image, int resize, boolean privacy) throws IOException {
        Log.w("Add attachment uri=" + uri + " image=" + image + " resize=" + resize + " privacy=" + privacy);

        NoStreamException.check(uri, context);

        EntityAttachment attachment = new EntityAttachment();
        UriInfo info = getInfo(uri, context);

        EntityLog.log(context, "Add attachment" +
                " uri=" + uri + " image=" + image + " resize=" + resize + " privacy=" + privacy +
                " name=" + info.name + " type=" + info.type + " size=" + info.size);

        String ext = Helper.getExtension(info.name);
        if (info.name != null && ext == null && info.type != null) {
            String guessed = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(info.type.toLowerCase(Locale.ROOT));
            if (!TextUtils.isEmpty(guessed)) {
                ext = guessed;
                info.name += '.' + ext;
            }
        }

        DB db = DB.getInstance(context);
        try {
            db.beginTransaction();

            EntityMessage draft = db.message().getMessage(id);
            if (draft == null)
                return null;

            Log.i("Attaching to id=" + id);

            attachment.message = draft.id;
            attachment.sequence = db.attachment().getAttachmentSequence(draft.id) + 1;
            if (privacy)
                attachment.name = "img" + attachment.sequence + (ext == null ? "" : "." + ext);
            else
                attachment.name = info.name;
            attachment.type = info.type;
            attachment.disposition = (image ? Part.INLINE : Part.ATTACHMENT);
            attachment.size = info.size;
            attachment.progress = 0;

            attachment.id = db.attachment().insertAttachment(attachment);
            Log.i("Created attachment=" + attachment.name + ":" + attachment.sequence + " type=" + attachment.type);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        long size = 0;
        int lastProgress = 0;
        try {
            File file = attachment.getFile(context);

            InputStream is = null;
            OutputStream os = null;
            try {
                is = context.getContentResolver().openInputStream(uri);
                os = new FileOutputStream(file);

                if (is == null)
                    throw new FileNotFoundException(uri.toString());

                byte[] buffer = new byte[Helper.BUFFER_SIZE];
                for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
                    size += len;
                    os.write(buffer, 0, len);

                    // Update progress
                    if (attachment.size != null && attachment.size > 0) {
                        int progress = (int) (size * 100 / attachment.size / 20 * 20);
                        if (progress != lastProgress) {
                            lastProgress = progress;
                            db.attachment().setProgress(attachment.id, progress);
                        }
                    }
                }

                if (image) {
                    attachment.cid = "<" + BuildConfig.APPLICATION_ID + "." + attachment.id + ">";
                    attachment.related = true;
                    db.attachment().setCid(attachment.id, attachment.cid, attachment.related);
                }
            } finally {
                try {
                    if (is != null)
                        is.close();
                } finally {
                    if (os != null)
                        os.close();
                }
            }

            db.attachment().setDownloaded(attachment.id, size);

            if (BuildConfig.APPLICATION_ID.equals(uri.getAuthority()) &&
                    uri.getPathSegments().size() > 0 &&
                    "photo".equals(uri.getPathSegments().get(0))) {
                // content://eu.faircode.email/photo/nnn.jpg
                File tmp = new File(context.getFilesDir(), uri.getPath());
                Log.i("Deleting " + tmp);
                if (!tmp.delete())
                    Log.w("Error deleting " + tmp);
            } else
                Log.i("Authority=" + uri.getAuthority());

            if (resize > 0)
                resizeAttachment(context, attachment, resize);

            if (privacy && resize == 0)
                try {
                    ExifInterface exif = new ExifInterface(file);

                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, null);
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null);
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, null);
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null);
                    exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, null);
                    exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, null);
                    exif.setAttribute(ExifInterface.TAG_GPS_SPEED_REF, null);
                    exif.setAttribute(ExifInterface.TAG_GPS_SPEED, null);

                    exif.setAttribute(ExifInterface.TAG_GPS_DEST_LATITUDE_REF, null);
                    exif.setAttribute(ExifInterface.TAG_GPS_DEST_LATITUDE, null);
                    exif.setAttribute(ExifInterface.TAG_GPS_DEST_LONGITUDE_REF, null);
                    exif.setAttribute(ExifInterface.TAG_GPS_DEST_LONGITUDE, null);
                    exif.setAttribute(ExifInterface.TAG_GPS_DEST_BEARING_REF, null);
                    exif.setAttribute(ExifInterface.TAG_GPS_DEST_BEARING, null);
                    exif.setAttribute(ExifInterface.TAG_GPS_DEST_DISTANCE_REF, null);
                    exif.setAttribute(ExifInterface.TAG_GPS_DEST_DISTANCE, null);

                    exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, null);
                    exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION, null);

                    exif.setAttribute(ExifInterface.TAG_GPS_TRACK_REF, null);
                    exif.setAttribute(ExifInterface.TAG_GPS_TRACK, null);

                    exif.setAttribute(ExifInterface.TAG_GPS_AREA_INFORMATION, null);

                    exif.setAttribute(ExifInterface.TAG_DATETIME, null);
                    exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, null);
                    exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, null);

                    exif.setAttribute(ExifInterface.TAG_XMP, null);
                    exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, null);
                    exif.setAttribute(ExifInterface.TAG_ARTIST, null);
                    exif.setAttribute(ExifInterface.TAG_IMAGE_UNIQUE_ID, null);
                    exif.setAttribute(ExifInterface.TAG_CAMERA_OWNER_NAME, null);
                    exif.setAttribute(ExifInterface.TAG_BODY_SERIAL_NUMBER, null);
                    exif.setAttribute(ExifInterface.TAG_LENS_SERIAL_NUMBER, null);

                    exif.saveAttributes();
                } catch (IOException ex) {
                    Log.i(ex);
                }

        } catch (Throwable ex) {
            // Reset progress on failure
            Log.e(ex);
            db.attachment().setError(attachment.id, Log.formatThrowable(ex, false));
            return null;
        }

        return attachment;
    }

    private static void resizeAttachment(Context context, EntityAttachment attachment, int resize) throws IOException {
        File file = attachment.getFile(context);
        if (file.exists() /* upload cancelled */ &&
                ("image/jpeg".equals(attachment.type) || "image/png".equals(attachment.type))) {
            ExifInterface exifSaved;
            try {
                exifSaved = new ExifInterface(file);
            } catch (Throwable ex) {
                Log.w(ex);
                exifSaved = null;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);

            int factor = 1;
            while (options.outWidth / factor > resize ||
                    options.outHeight / factor > resize)
                factor *= 2;

            Matrix rotation = ("image/jpeg".equals(attachment.type) ? ImageHelper.getImageRotation(file) : null);
            Log.i("Image type=" + attachment.type + " rotation=" + rotation);
            if (factor > 1 || rotation != null) {
                options.inJustDecodeBounds = false;
                options.inSampleSize = factor;

                Log.i("Image target size=" + resize + " factor=" + factor + " source=" + options.outWidth + "x" + options.outHeight);
                Bitmap resized = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                if (resized != null) {
                    Log.i("Image result size=" + resized.getWidth() + "x" + resized.getHeight() + " rotation=" + rotation);

                    if (rotation != null) {
                        Bitmap rotated = Bitmap.createBitmap(resized, 0, 0, resized.getWidth(), resized.getHeight(), rotation, true);
                        resized.recycle();
                        resized = rotated;
                    }

                    File tmp = new File(file.getAbsolutePath() + ".tmp");
                    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp))) {
                        Bitmap.CompressFormat format = ("image/jpeg".equals(attachment.type)
                                ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.PNG);
                        if (!resized.compress(format, REDUCED_IMAGE_QUALITY, out))
                            throw new IOException("compress");
                    } catch (Throwable ex) {
                        Log.w(ex);
                        tmp.delete();
                    } finally {
                        resized.recycle();
                    }

                    if (tmp.exists() && tmp.length() > 0) {
                        file.delete();
                        tmp.renameTo(file);
                    }

                    DB db = DB.getInstance(context);
                    db.attachment().setDownloaded(attachment.id, file.length());

                    if (exifSaved != null)
                        try {
                            ExifInterface exif = new ExifInterface(file);

                            // Preserve time
                            if (exifSaved.hasAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
                                exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL,
                                        exifSaved.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL));
                            if (exifSaved.hasAttribute(ExifInterface.TAG_GPS_DATESTAMP))
                                exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP,
                                        exifSaved.getAttribute(ExifInterface.TAG_GPS_DATESTAMP));

                            // Preserve location
                            double[] latlong = exifSaved.getLatLong();
                            if (latlong != null)
                                exif.setLatLong(latlong[0], latlong[1]);

                            // Preserve altitude
                            if (exifSaved.hasAttribute(ExifInterface.TAG_GPS_ALTITUDE) &&
                                    exifSaved.hasAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF))
                                exif.setAltitude(exifSaved.getAltitude(0));

                            exif.saveAttributes();
                        } catch (Throwable ex) {
                            Log.w(ex);
                        }
                }
            }
        }
    }

    private SimpleTask<DraftData> draftLoader = new SimpleTask<DraftData>() {
        @Override
        protected DraftData onExecute(Context context, Bundle args) throws Throwable {
            String action = args.getString("action");
            long id = args.getLong("id", -1);
            long aid = args.getLong("account", -1);
            long iid = args.getLong("identity", -1);
            long reference = args.getLong("reference", -1);
            int dsn = args.getInt("dsn", EntityMessage.DSN_RECEIPT);
            File ics = (File) args.getSerializable("ics");
            String status = args.getString("status");
            // raw
            long answer = args.getLong("answer", -1);
            String to = args.getString("to");
            String cc = args.getString("cc");
            String bcc = args.getString("bcc");
            // inreplyto
            String external_subject = args.getString("subject", "");
            String external_body = args.getString("body", "");
            String external_text = args.getString("text");
            CharSequence selected_text = args.getCharSequence("selected");
            ArrayList<Uri> uris = args.getParcelableArrayList("attachments");

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean plain_only = prefs.getBoolean("plain_only", false);
            boolean resize_reply = prefs.getBoolean("resize_reply", true);
            boolean sign_default = prefs.getBoolean("sign_default", false);
            boolean encrypt_default = prefs.getBoolean("encrypt_default", false);
            boolean receipt_default = prefs.getBoolean("receipt_default", false);
            boolean write_below = prefs.getBoolean("write_below", false);
            boolean save_drafts = prefs.getBoolean("save_drafts", true);
            boolean auto_identity = prefs.getBoolean("auto_identity", false);
            boolean suggest_sent = prefs.getBoolean("suggest_sent", true);
            boolean suggest_received = prefs.getBoolean("suggest_received", false);
            boolean forward_new = prefs.getBoolean("forward_new", true);

            Log.i("Load draft action=" + action + " id=" + id + " reference=" + reference);

            Map<String, String> crumb = new HashMap<>();
            crumb.put("draft", Long.toString(id));
            crumb.put("reference", Long.toString(reference));
            crumb.put("action", action);
            Log.breadcrumb("compose", crumb);

            DraftData data = new DraftData();

            DB db = DB.getInstance(context);
            try {
                db.beginTransaction();

                data.identities = db.identity().getComposableIdentities(null);
                if (data.identities == null || data.identities.size() == 0)
                    throw new OperationCanceledException(context.getString(R.string.title_no_composable));

                data.draft = db.message().getMessage(id);
                if (data.draft == null || data.draft.ui_hide) {
                    // New draft
                    if ("edit".equals(action))
                        throw new MessageRemovedException("Draft for edit was deleted hide=" + (data.draft != null));

                    EntityMessage ref = db.message().getMessage(reference);

                    data.draft = new EntityMessage();
                    data.draft.msgid = EntityMessage.generateMessageId();

                    // Select identity matching from address
                    EntityIdentity selected = null;

                    if (aid < 0)
                        if (ref == null) {
                            EntityAccount primary = db.account().getPrimaryAccount();
                            if (primary != null)
                                aid = primary.id;
                        } else
                            aid = ref.account;
                    if (iid < 0 && ref != null && ref.identity != null)
                        iid = ref.identity;

                    if (iid >= 0)
                        for (EntityIdentity identity : data.identities)
                            if (identity.id.equals(iid)) {
                                selected = identity;
                                EntityLog.log(context, "Selected requested identity=" + iid);
                                break;
                            }

                    if (ref != null) {
                        Address[] refto;
                        boolean self = ref.replySelf(data.identities, ref.account);
                        if (ref.to == null || ref.to.length == 0 || self)
                            refto = ref.from;
                        else
                            refto = ref.to;
                        Log.i("Ref self=" + self +
                                " to=" + MessageHelper.formatAddresses(refto));
                        if (refto != null && refto.length > 0) {
                            if (selected == null)
                                for (Address sender : refto)
                                    for (EntityIdentity identity : data.identities)
                                        if (identity.account.equals(aid) &&
                                                identity.sameAddress(sender)) {
                                            selected = identity;
                                            EntityLog.log(context, "Selected same account/identity");
                                            break;
                                        }

                            if (selected == null)
                                for (Address sender : refto)
                                    for (EntityIdentity identity : data.identities)
                                        if (identity.account.equals(aid) &&
                                                identity.similarAddress(sender)) {
                                            selected = identity;
                                            EntityLog.log(context, "Selected similar account/identity");
                                            break;
                                        }

                            if (selected == null)
                                for (Address sender : refto)
                                    for (EntityIdentity identity : data.identities)
                                        if (identity.sameAddress(sender)) {
                                            selected = identity;
                                            EntityLog.log(context, "Selected same */identity");
                                            break;
                                        }

                            if (selected == null)
                                for (Address sender : refto)
                                    for (EntityIdentity identity : data.identities)
                                        if (identity.similarAddress(sender)) {
                                            selected = identity;
                                            EntityLog.log(context, "Selected similer */identity");
                                            break;
                                        }
                        }
                    }

                    if (selected == null && auto_identity)
                        try {
                            Address[] tos = MessageHelper.parseAddresses(context, to);
                            if (tos != null && tos.length > 0) {
                                String email = ((InternetAddress) tos[0]).getAddress();
                                List<Integer> types = new ArrayList<>();
                                if (suggest_sent)
                                    types.add(EntityContact.TYPE_TO);
                                if (suggest_received)
                                    types.add(EntityContact.TYPE_FROM);
                                List<Long> identities = db.contact().getIdentities(email, types);
                                if (identities != null && identities.size() == 1) {
                                    EntityIdentity identity = db.identity().getIdentity(identities.get(0));
                                    if (identity != null)
                                        selected = identity;
                                }
                            }
                        } catch (AddressException ex) {
                            Log.i(ex);
                        }

                    if (selected == null)
                        for (EntityIdentity identity : data.identities)
                            if (identity.account.equals(aid) && identity.primary) {
                                selected = identity;
                                EntityLog.log(context, "Selected primary account/identity");
                                break;
                            }

                    if (selected == null)
                        for (EntityIdentity identity : data.identities)
                            if (identity.account.equals(aid)) {
                                selected = identity;
                                EntityLog.log(context, "Selected account/identity");
                                break;
                            }

                    if (selected == null)
                        for (EntityIdentity identity : data.identities)
                            if (identity.primary) {
                                selected = identity;
                                EntityLog.log(context, "Selected primary */identity");
                                break;
                            }

                    if (selected == null)
                        for (EntityIdentity identity : data.identities) {
                            selected = identity;
                            EntityLog.log(context, "Selected */identity");
                            break;
                        }

                    if (selected == null)
                        throw new OperationCanceledException(context.getString(R.string.title_no_composable));

                    EntityLog.log(context, "Selected=" + selected.email);

                    if (!"dsn".equals(action)) {
                        if (plain_only &&
                                !"resend".equals(action) &&
                                !"editasnew".equals(action))
                            data.draft.plain_only = 1;

                        if (encrypt_default || selected.encrypt_default)
                            if (selected.encrypt == 0)
                                data.draft.ui_encrypt = EntityMessage.PGP_SIGNENCRYPT;
                            else
                                data.draft.ui_encrypt = EntityMessage.SMIME_SIGNENCRYPT;
                        else if (sign_default || selected.sign_default)
                            if (selected.encrypt == 0)
                                data.draft.ui_encrypt = EntityMessage.PGP_SIGNONLY;
                            else
                                data.draft.ui_encrypt = EntityMessage.SMIME_SIGNONLY;
                    }

                    if (receipt_default)
                        data.draft.receipt_request = true;

                    Document document = Document.createShell("");

                    if (ref == null) {
                        data.draft.thread = data.draft.msgid;

                        try {
                            data.draft.to = MessageHelper.parseAddresses(context, to);
                        } catch (AddressException ex) {
                            Log.w(ex);
                        }

                        try {
                            data.draft.cc = MessageHelper.parseAddresses(context, cc);
                        } catch (AddressException ex) {
                            Log.w(ex);
                        }

                        try {
                            data.draft.bcc = MessageHelper.parseAddresses(context, bcc);
                        } catch (AddressException ex) {
                            Log.w(ex);
                        }

                        data.draft.inreplyto = args.getString("inreplyto", null);

                        data.draft.subject = external_subject;

                        if (!TextUtils.isEmpty(external_body)) {
                            Document d = JsoupEx.parse(external_body); // Passed html
                            Element e = document
                                    .createElement("div")
                                    .html(d.body().html());
                            document.body().appendChild(e);
                        }

                        EntityAnswer a = (answer < 0
                                ? db.answer().getStandardAnswer()
                                : db.answer().getAnswer(answer));
                        if (a != null) {
                            db.answer().applyAnswer(a.id, new Date().getTime());
                            if (answer > 0)
                                data.draft.subject = a.name;
                            if (TextUtils.isEmpty(external_body)) {
                                Document d = JsoupEx.parse(a.getHtml(context, null));
                                document.body().append(d.body().html());
                            }
                        }

                        data.draft.signature = prefs.getBoolean("signature_new", true);
                        addSignature(context, document, data.draft, selected);
                    } else {
                        // Actions:
                        // - reply
                        // - reply_all
                        // - forward
                        // - resend
                        // - editasnew
                        // - list
                        // - dsn
                        // - receipt
                        // - participation

                        // References
                        if ("reply".equals(action) || "reply_all".equals(action) ||
                                "list".equals(action) ||
                                "dsn".equals(action) ||
                                "participation".equals(action)) {
                            // https://tools.ietf.org/html/rfc5322#section-3.6.4
                            // The "References:" field will contain the contents of the parent's "References:" field (if any)
                            // followed by the contents of the parent's "Message-ID:" field (if any).
                            String refs = (ref.references == null ? "" : ref.references);
                            if (!TextUtils.isEmpty(ref.msgid))
                                refs = (TextUtils.isEmpty(refs) ? ref.msgid : refs + " " + ref.msgid);
                            data.draft.references = refs;
                            data.draft.inreplyto = ref.msgid;
                            data.draft.thread = ref.thread;

                            if ("list".equals(action) && ref.list_post != null)
                                data.draft.to = ref.list_post;
                            else if ("dsn".equals(action)) {
                                if (EntityMessage.DSN_RECEIPT.equals(dsn)) {
                                    if (ref.receipt_to != null)
                                        data.draft.to = ref.receipt_to;
                                } else if (EntityMessage.DSN_HARD_BOUNCE.equals(dsn)) {
                                    if (ref.return_path != null)
                                        data.draft.to = ref.return_path;
                                }
                            } else {
                                // Prevent replying to self
                                if (ref.replySelf(data.identities, ref.account)) {
                                    EntityLog.log(context, "Reply self ref" +
                                            " from=" + MessageHelper.formatAddresses(ref.from) +
                                            " to=" + MessageHelper.formatAddresses(ref.to));
                                    data.draft.from = ref.from;
                                    data.draft.to = ref.to;
                                } else {
                                    data.draft.from = ref.to;
                                    data.draft.to = (ref.reply == null || ref.reply.length == 0 ? ref.from : ref.reply);
                                }

                                if (data.draft.from != null && data.draft.from.length > 0) {
                                    Address preferred = null;
                                    if (ref.identity != null) {
                                        EntityIdentity recognized = db.identity().getIdentity(ref.identity);
                                        EntityLog.log(context, "Recognized=" + (recognized == null ? null : recognized.email));

                                        if (recognized != null) {
                                            Address same = null;
                                            Address similar = null;

                                            for (Address from : data.draft.from) {
                                                if (same == null && recognized.sameAddress(from))
                                                    same = from;
                                                if (similar == null && recognized.similarAddress(from))
                                                    similar = from;
                                            }

                                            //if (ref.deliveredto != null)
                                            //    try {
                                            //        Address deliveredto = new InternetAddress(ref.deliveredto);
                                            //        if (same == null && recognized.sameAddress(deliveredto))
                                            //            same = deliveredto;
                                            //        if (similar == null && recognized.similarAddress(deliveredto))
                                            //            similar = deliveredto;
                                            //    } catch (AddressException ex) {
                                            //        Log.w(ex);
                                            //    }

                                            EntityLog.log(context, "From=" + MessageHelper.formatAddresses(data.draft.from) +
                                                    " delivered-to=" + ref.deliveredto +
                                                    " same=" + (same == null ? null : ((InternetAddress) same).getAddress()) +
                                                    " similar=" + (similar == null ? null : ((InternetAddress) similar).getAddress()));

                                            preferred = (same == null ? similar : same);
                                        }
                                    } else
                                        EntityLog.log(context, "Recognized=null");

                                    if (preferred != null) {
                                        String from = ((InternetAddress) preferred).getAddress();
                                        EntityLog.log(context, "Preferred=" + from);
                                        data.draft.extra = UriHelper.getEmailUser(from);
                                    } else
                                        EntityLog.log(context, "Preferred=null");
                                }
                            }

                            if ("reply_all".equals(action))
                                data.draft.cc = ref.getAllRecipients(data.identities, ref.account);
                            else if ("dsn".equals(action)) {
                                data.draft.dsn = dsn;
                                data.draft.receipt_request = false;
                            }

                        } else if ("forward".equals(action)) {
                            if (forward_new)
                                data.draft.thread = data.draft.msgid; // new thread
                            else {
                                data.draft.thread = ref.thread;
                                data.draft.inreplyto = ref.msgid;
                                data.draft.references = (ref.references == null ? "" : ref.references + " ") + ref.msgid;
                            }
                            data.draft.wasforwardedfrom = ref.msgid;
                        } else if ("resend".equals(action)) {
                            data.draft.resend = true;
                            data.draft.thread = data.draft.msgid;
                            data.draft.headers = ref.headers;
                        } else if ("editasnew".equals(action))
                            data.draft.thread = data.draft.msgid;

                        // Subject
                        String subject = (ref.subject == null ? "" : ref.subject);
                        if ("reply".equals(action) || "reply_all".equals(action)) {
                            data.draft.subject =
                                    EntityMessage.getSubject(context, ref.language, subject, false);

                            if (external_text != null) {
                                Element div = document.createElement("div");
                                for (String line : external_text.split("\\r?\\n")) {
                                    Element span = document.createElement("span");
                                    span.text(line);
                                    div.appendChild(span);
                                    div.appendElement("br");
                                }
                                document.body().appendChild(div);
                            }
                        } else if ("forward".equals(action)) {
                            data.draft.subject =
                                    EntityMessage.getSubject(context, ref.language, subject, true);
                        } else if ("resend".equals(action)) {
                            data.draft.subject = ref.subject;
                        } else if ("editasnew".equals(action)) {
                            if (ref.from != null && ref.from.length == 1) {
                                String from = ((InternetAddress) ref.from[0]).getAddress();
                                for (EntityIdentity identity : data.identities)
                                    if (identity.email.equals(from)) {
                                        selected = identity;
                                        break;
                                    }
                            }

                            data.draft.to = ref.to;
                            data.draft.cc = ref.cc;
                            data.draft.bcc = ref.bcc;
                            data.draft.subject = ref.subject;

                            if (ref.content)
                                document = JsoupEx.parse(ref.getFile(context));
                        } else if ("list".equals(action)) {
                            data.draft.subject = ref.subject;
                        } else if ("dsn".equals(action)) {
                            if (EntityMessage.DSN_HARD_BOUNCE.equals(dsn))
                                data.draft.subject = context.getString(R.string.title_hard_bounce_subject);
                            else
                                data.draft.subject = context.getString(R.string.title_receipt_subject, subject);

                            String[] texts;
                            if (EntityMessage.DSN_HARD_BOUNCE.equals(dsn))
                                texts = new String[]{context.getString(R.string.title_hard_bounce_text)};
                            else {
                                EntityAnswer receipt = db.answer().getReceiptAnswer();
                                if (receipt == null)
                                    texts = Helper.getStrings(context, ref.language, R.string.title_receipt_text);
                                else {
                                    db.answer().applyAnswer(receipt.id, new Date().getTime());
                                    texts = new String[0];
                                    Document d = JsoupEx.parse(receipt.getHtml(context, null));
                                    document.body().append(d.body().html());
                                }
                            }

                            for (int i = 0; i < texts.length; i++) {
                                if (i > 0)
                                    document.body()
                                            .appendElement("br");

                                Element div = document.createElement("div");
                                div.text(texts[i]);
                                document.body()
                                        .appendChild(div)
                                        .appendElement("br");
                            }
                        } else if ("participation".equals(action))
                            data.draft.subject = status + ": " + ref.subject;

                        if (!"dsn".equals(action)) {
                            // Sensitivity
                            data.draft.sensitivity = ref.sensitivity;

                            // Plain-only
                            if (ref.isPlainOnly())
                                data.draft.plain_only = 1;

                            // Encryption
                            List<Address> recipients = new ArrayList<>();
                            if (data.draft.to != null)
                                recipients.addAll(Arrays.asList(data.draft.to));
                            if (data.draft.cc != null)
                                recipients.addAll(Arrays.asList(data.draft.cc));
                            if (data.draft.bcc != null)
                                recipients.addAll(Arrays.asList(data.draft.bcc));

                            if (EntityMessage.PGP_SIGNONLY.equals(ref.ui_encrypt) ||
                                    EntityMessage.PGP_SIGNENCRYPT.equals(ref.ui_encrypt)) {
                                if (Helper.isOpenKeychainInstalled(context) &&
                                        selected.sign_key != null &&
                                        (EntityMessage.PGP_SIGNENCRYPT.equals(ref.ui_encrypt) ||
                                                PgpHelper.hasPgpKey(context, recipients)))
                                    data.draft.ui_encrypt = ref.ui_encrypt;
                            } else if (EntityMessage.SMIME_SIGNONLY.equals(ref.ui_encrypt) ||
                                    EntityMessage.SMIME_SIGNENCRYPT.equals(ref.ui_encrypt)) {
                                if (ActivityBilling.isPro(context) &&
                                        selected.sign_key_alias != null &&
                                        (EntityMessage.SMIME_SIGNENCRYPT.equals(ref.ui_encrypt) ||
                                                SmimeHelper.hasSmimeKey(context, recipients)))
                                    data.draft.ui_encrypt = ref.ui_encrypt;
                            }
                        }

                        // Reply template
                        EntityAnswer a = null;
                        if (answer < 0) {
                            if ("reply".equals(action) || "reply_all".equals(action) ||
                                    "forward".equals(action) || "list".equals(action))
                                a = db.answer().getStandardAnswer();
                        } else
                            a = db.answer().getAnswer(answer);

                        if (a != null) {
                            db.answer().applyAnswer(a.id, new Date().getTime());
                            Document d = JsoupEx.parse(a.getHtml(context, data.draft.to));
                            document.body().append(d.body().html());
                        }

                        // Signature
                        if ("reply".equals(action) || "reply_all".equals(action))
                            data.draft.signature = prefs.getBoolean("signature_reply", true);
                        else if ("forward".equals(action))
                            data.draft.signature = prefs.getBoolean("signature_forward", true);
                        else
                            data.draft.signature = false;

                        if (ref.content && "resend".equals(action)) {
                            document = JsoupEx.parse(ref.getFile(context));
                            // Save original body
                            Element div = document.body()
                                    .tagName("div")
                                    .attr("fairemail", "reference");
                            Element body = document.createElement("body")
                                    .appendChild(div);
                            document.body().replaceWith(body);
                        }

                        // Reply header
                        if (ref.content &&
                                !"resend".equals(action) &&
                                !"editasnew".equals(action) &&
                                !("list".equals(action) && TextUtils.isEmpty(selected_text)) &&
                                !"dsn".equals(action)) {
                            // Reply/forward
                            Element reply = document.createElement("div");
                            reply.attr("fairemail", "reference");

                            reply.appendElement("br");

                            // Build reply header
                            boolean separate_reply = prefs.getBoolean("separate_reply", false);
                            boolean extended_reply = prefs.getBoolean("extended_reply", false);
                            Element p = ref.getReplyHeader(context, document, separate_reply, extended_reply);
                            reply.appendChild(p);

                            Document d;
                            if (TextUtils.isEmpty(selected_text)) {
                                // Get referenced message body
                                d = JsoupEx.parse(ref.getFile(context));
                                HtmlHelper.normalizeNamespaces(d, false);
                                HtmlHelper.clearAnnotations(d); // Legacy left-overs

                                if (BuildConfig.DEBUG)
                                    d.select(".faircode_remove").remove();

                                if ("reply".equals(action) || "reply_all".equals(action)) {
                                    // Remove signature separators
                                    boolean remove_signatures = prefs.getBoolean("remove_signatures", false);
                                    if (remove_signatures)
                                        d.body().filter(new NodeFilter() {
                                            private boolean remove = false;
                                            private boolean noremove = false;

                                            @Override
                                            public FilterResult head(Node node, int depth) {
                                                if (node instanceof TextNode) {
                                                    TextNode tnode = (TextNode) node;
                                                    String text = tnode.getWholeText()
                                                            .replaceAll("[\r\n]+$", "")
                                                            .replaceAll("^[\r\n]+", "");
                                                    if ("-- ".equals(text)) {
                                                        if (tnode.getWholeText().endsWith("\n"))
                                                            remove = true;
                                                        else {
                                                            Node next = node.nextSibling();
                                                            if (next == null) {
                                                                Node parent = node.parent();
                                                                if (parent != null)
                                                                    next = parent.nextSibling();
                                                            }
                                                            if (next != null && "br".equals(next.nodeName()))
                                                                remove = true;
                                                        }
                                                    }
                                                } else if (node instanceof Element) {
                                                    Element element = (Element) node;
                                                    if (remove && "blockquote".equals(element.tagName()))
                                                        noremove = true;
                                                }

                                                return (remove && !noremove
                                                        ? FilterResult.REMOVE : FilterResult.CONTINUE);
                                            }

                                            @Override
                                            public FilterResult tail(Node node, int depth) {
                                                return FilterResult.CONTINUE;
                                            }
                                        });

                                    // Limit number of nested block quotes
                                    boolean quote_limit = prefs.getBoolean("quote_limit", true);
                                    if (quote_limit)
                                        HtmlHelper.quoteLimit(d, MAX_QUOTE_LEVEL);
                                }
                            } else {
                                // Selected text
                                d = Document.createShell("");

                                Element div = d.createElement("div");
                                if (selected_text instanceof Spanned)
                                    div.html(HtmlHelper.toHtml((Spanned) selected_text, context));
                                else
                                    for (String line : selected_text.toString().split("\\r?\\n")) {
                                        Element span = document.createElement("span");
                                        span.text(line);
                                        div.appendChild(span);
                                        div.appendElement("br");
                                    }

                                d.body().appendChild(div);
                            }

                            Element e = d.body();

                            // Apply styles
                            List<CSSStyleSheet> sheets = HtmlHelper.parseStyles(d.head().select("style"));
                            for (Element element : e.select("*")) {
                                String tag = element.tagName();
                                String clazz = element.attr("class");
                                String style = HtmlHelper.processStyles(tag, clazz, null, sheets);
                                style = HtmlHelper.mergeStyles(style, element.attr("style"));
                                if (!TextUtils.isEmpty(style))
                                    element.attr("style", style);
                            }

                            // Quote referenced message body
                            boolean quote_reply = prefs.getBoolean("quote_reply", true);
                            boolean quote = (quote_reply &&
                                    ("reply".equals(action) || "reply_all".equals(action) || "list".equals(action)));

                            if (quote) {
                                String style = e.attr("style");
                                style = HtmlHelper.mergeStyles(style, HtmlHelper.getQuoteStyle(e));
                                e.tagName("blockquote").attr("style", style);
                            } else
                                e.tagName("p");
                            reply.appendChild(e);

                            if (write_below && data.draft.wasforwardedfrom == null)
                                document.body().prependChild(reply);
                            else
                                document.body().appendChild(reply);

                            addSignature(context, document, data.draft, selected);
                        }
                    }

                    EntityFolder drafts = db.folder().getFolderByType(selected.account, EntityFolder.DRAFTS);
                    if (drafts == null)
                        throw new IllegalArgumentException(context.getString(R.string.title_no_drafts));

                    boolean signature_once = prefs.getBoolean("signature_reply_once", false);
                    if (signature_once && data.draft.signature &&
                            ref != null && ref.thread != null &&
                            ("reply".equals(action) || "reply_all".equals(action))) {
                        List<EntityMessage> outbound = new ArrayList<>();

                        EntityFolder sent = db.folder().getFolderByType(drafts.account, EntityFolder.SENT);
                        if (sent != null)
                            outbound.addAll(db.message().getMessagesByThread(drafts.account, ref.thread, null, sent.id));

                        EntityFolder outbox = db.folder().getOutbox();
                        if (outbox != null)
                            outbound.addAll(db.message().getMessagesByThread(drafts.account, ref.thread, null, outbox.id));

                        if (outbound.size() > 0) {
                            Log.i("Signature suppressed");
                            data.draft.signature = false;
                        }
                    }

                    data.draft.account = drafts.account;
                    data.draft.folder = drafts.id;
                    data.draft.identity = selected.id;
                    data.draft.from = new InternetAddress[]{new InternetAddress(selected.email, selected.name, StandardCharsets.UTF_8.name())};

                    data.draft.sender = MessageHelper.getSortKey(data.draft.from);
                    Uri lookupUri = ContactInfo.getLookupUri(data.draft.from);
                    data.draft.avatar = (lookupUri == null ? null : lookupUri.toString());

                    data.draft.received = new Date().getTime();
                    data.draft.seen = true;
                    data.draft.ui_seen = true;

                    data.draft.revision = 1;
                    data.draft.revisions = 1;

                    data.draft.id = db.message().insertMessage(data.draft);

                    String html = document.html();
                    Helper.writeText(data.draft.getFile(context), html);
                    Helper.writeText(data.draft.getFile(context, data.draft.revision), html);

                    String text = HtmlHelper.getFullText(html);
                    data.draft.preview = HtmlHelper.getPreview(text);
                    data.draft.language = HtmlHelper.getLanguage(context, data.draft.subject, text);
                    db.message().setMessageContent(data.draft.id,
                            true,
                            data.draft.language,
                            data.draft.plain_only,
                            data.draft.preview,
                            null);

                    if ("participation".equals(action)) {
                        EntityAttachment attachment = new EntityAttachment();
                        attachment.message = data.draft.id;
                        attachment.sequence = 1;
                        attachment.name = "meeting.ics";
                        attachment.type = "text/calendar";
                        attachment.disposition = Part.ATTACHMENT;
                        attachment.size = ics.length();
                        attachment.progress = null;
                        attachment.available = true;
                        attachment.id = db.attachment().insertAttachment(attachment);
                        File file = attachment.getFile(context);
                        ics.renameTo(file);

                        ICalendar icalendar = Biweekly.parse(file).first();
                        VEvent event = icalendar.getEvents().get(0);
                        Organizer organizer = event.getOrganizer();
                        if (organizer != null) {
                            String email = organizer.getEmail();
                            String name = organizer.getCommonName();
                            if (!TextUtils.isEmpty(email)) {
                                InternetAddress o = new InternetAddress(email, name, StandardCharsets.UTF_8.name());
                                Log.i("Setting organizer=" + o);
                                data.draft.to = new Address[]{o};
                            }
                        }
                    }

                    if ("new".equals(action) && uris != null) {
                        ArrayList<Uri> images = new ArrayList<>();
                        for (Uri uri : uris)
                            try {
                                UriInfo info = getInfo(uri, context);
                                if (info.isImage())
                                    images.add(uri);
                                else
                                    addAttachment(context, data.draft.id, uri, false, 0, false);
                            } catch (IOException ex) {
                                Log.e(ex);
                            }

                        if (images.size() > 0)
                            args.putParcelableArrayList("images", images);
                    }

                    if (ref != null &&
                            ("reply".equals(action) || "reply_all".equals(action) ||
                                    "forward".equals(action) ||
                                    "resend".equals(action) ||
                                    "editasnew".equals(action))) {
                        List<String> cid = new ArrayList<>();
                        for (Element img : document.select("img")) {
                            String src = img.attr("src");
                            if (src.startsWith("cid:"))
                                cid.add("<" + src.substring(4) + ">");
                        }

                        int sequence = 0;
                        List<EntityAttachment> attachments = db.attachment().getAttachments(ref.id);
                        for (EntityAttachment attachment : attachments)
                            if (attachment.subsequence == null &&
                                    !attachment.isEncryption() &&
                                    (cid.contains(attachment.cid) ||
                                            !("reply".equals(action) || "reply_all".equals(action)))) {
                                if (attachment.available) {
                                    File source = attachment.getFile(context);

                                    if (cid.contains(attachment.cid))
                                        attachment.disposition = Part.INLINE;
                                    else {
                                        attachment.cid = null;
                                        attachment.related = false;
                                        attachment.disposition = Part.ATTACHMENT;
                                    }

                                    attachment.id = null;
                                    attachment.message = data.draft.id;
                                    attachment.sequence = ++sequence;
                                    attachment.id = db.attachment().insertAttachment(attachment);

                                    File target = attachment.getFile(context);
                                    Helper.copy(source, target);

                                    if (resize_reply &&
                                            ("reply".equals(action) || "reply_all".equals(action)))
                                        resizeAttachment(context, attachment, REDUCED_IMAGE_SIZE);
                                } else
                                    args.putBoolean("incomplete", true);
                            }
                    }

                    if (save_drafts &&
                            (data.draft.ui_encrypt == null ||
                                    EntityMessage.ENCRYPT_NONE.equals(data.draft.ui_encrypt)) &&
                            (!"new".equals(action) ||
                                    answer > 0 ||
                                    !TextUtils.isEmpty(to) ||
                                    !TextUtils.isEmpty(cc) ||
                                    !TextUtils.isEmpty(bcc) ||
                                    !TextUtils.isEmpty(external_subject) ||
                                    !TextUtils.isEmpty(external_body) ||
                                    !TextUtils.isEmpty(external_text) ||
                                    !TextUtils.isEmpty(selected_text) ||
                                    (uris != null && uris.size() > 0))) {
                        Map<String, String> c = new HashMap<>();
                        c.put("id", data.draft.id == null ? null : Long.toString(data.draft.id));
                        c.put("encrypt", data.draft.encrypt + "/" + data.draft.ui_encrypt);
                        c.put("action", action);
                        Log.breadcrumb("Load draft", c);

                        EntityOperation.queue(context, data.draft, EntityOperation.ADD);
                    }
                } else {
                    args.putBoolean("saved", true);

                    if (!data.draft.ui_seen)
                        EntityOperation.queue(context, data.draft, EntityOperation.SEEN, true);

                    // External draft
                    if (data.draft.identity == null) {
                        for (EntityIdentity identity : data.identities)
                            if (identity.account.equals(data.draft.account))
                                if (identity.primary) {
                                    data.draft.identity = identity.id;
                                    break;
                                } else if (data.draft.identity == null)
                                    data.draft.identity = identity.id;

                        if (data.draft.identity != null)
                            db.message().setMessageIdentity(data.draft.id, data.draft.identity);
                        Log.i("Selected external identity=" + data.draft.identity);
                    }

                    if (data.draft.revision == null || data.draft.revisions == null) {
                        data.draft.revision = 1;
                        data.draft.revisions = 1;
                        db.message().setMessageRevision(data.draft.id, data.draft.revision);
                        db.message().setMessageRevisions(data.draft.id, data.draft.revisions);
                    }

                    if (data.draft.content || data.draft.uid == null) {
                        if (data.draft.uid == null && !data.draft.content)
                            Log.e("Draft without uid");

                        File file = data.draft.getFile(context);

                        Document doc = (data.draft.content ? JsoupEx.parse(file) : Document.createShell(""));
                        doc.select("div[fairemail=signature]").remove();
                        Elements ref = doc.select("div[fairemail=reference]");
                        ref.remove();

                        File refFile = data.draft.getRefFile(context);
                        if (refFile.exists()) {
                            ref.html(Helper.readText(refFile));
                            refFile.delete();
                        }

                        // Possibly external draft

                        for (Element e : ref)
                            if (write_below && data.draft.wasforwardedfrom == null)
                                doc.body().prependChild(e);
                            else
                                doc.body().appendChild(e);

                        EntityIdentity identity = null;
                        if (data.draft.identity != null)
                            identity = db.identity().getIdentity(data.draft.identity);

                        addSignature(context, doc, data.draft, identity);

                        String html = doc.html();
                        Helper.writeText(file, html);
                        Helper.writeText(data.draft.getFile(context, data.draft.revision), html);

                        String text = HtmlHelper.getFullText(html);
                        data.draft.preview = HtmlHelper.getPreview(text);
                        data.draft.language = HtmlHelper.getLanguage(context, data.draft.subject, text);
                        db.message().setMessageContent(data.draft.id,
                                true,
                                data.draft.language,
                                data.draft.plain_only,
                                data.draft.preview,
                                null);
                    } else
                        EntityOperation.queue(context, data.draft, EntityOperation.BODY);
                }

                last_plain_only = data.draft.plain_only;
                last_attachments = db.attachment().getAttachments(data.draft.id);

                if (last_attachments != null)
                    for (EntityAttachment attachment : last_attachments)
                        if (!attachment.available && attachment.progress == null && attachment.error == null)
                            EntityOperation.queue(context, data.draft, EntityOperation.ATTACHMENT, attachment.id);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            ServiceSynchronize.eval(context, "compose/draft");

            return data;
        }

        @Override
        protected void onExecuted(Bundle args, final DraftData data) {
            final String action = getArguments().getString("action");
            Log.i("Loaded draft id=" + data.draft.id + " action=" + action);

            FragmentActivity activity = getActivity();
            if (activity != null) {
                Intent intent = activity.getIntent();
                if (intent != null) {
                    intent.putExtra("id", data.draft.id);
                    intent.putExtra("action", "edit");
                }
            }

            working = data.draft.id;
            dsn = (data.draft.dsn != null && !EntityMessage.DSN_NONE.equals(data.draft.dsn));
            encrypt = data.draft.ui_encrypt;
            invalidateOptionsMenu();

            subject = data.draft.subject;
            saved = args.getBoolean("saved");

            // Show identities
            AdapterIdentitySelect iadapter = new AdapterIdentitySelect(getContext(), data.identities);
            spIdentity.setAdapter(iadapter);

            // Select identity
            if (data.draft.identity != null)
                for (int pos = 0; pos < data.identities.size(); pos++) {
                    if (data.identities.get(pos).id.equals(data.draft.identity)) {
                        spIdentity.setTag(pos);
                        spIdentity.setSelection(pos);
                        break;
                    }
                }

            etExtra.setText(data.draft.extra);
            etTo.setText(MessageHelper.formatAddressesCompose(data.draft.to));
            etCc.setText(MessageHelper.formatAddressesCompose(data.draft.cc));
            etBcc.setText(MessageHelper.formatAddressesCompose(data.draft.bcc));
            etSubject.setText(data.draft.subject);

            long reference = args.getLong("reference", -1);
            etTo.setTag(reference < 0 ? "" : etTo.getText().toString());
            etSubject.setTag(reference < 0 ? "" : etSubject.getText().toString());
            cbSignature.setTag(data.draft.signature);

            grpHeader.setVisibility(View.VISIBLE);
            if ("reply_all".equals(action) ||
                    (data.draft.cc != null && data.draft.cc.length > 0) ||
                    (data.draft.bcc != null && data.draft.bcc.length > 0))
                grpAddresses.setVisibility(View.VISIBLE);
            ibCcBcc.setVisibility(View.VISIBLE);

            bottom_navigation.getMenu().findItem(R.id.action_undo).setVisible(data.draft.revision > 1);
            bottom_navigation.getMenu().findItem(R.id.action_redo).setVisible(data.draft.revision < data.draft.revisions);

            if (args.getBoolean("incomplete")) {
                final Snackbar snackbar = Snackbar.make(
                                view, R.string.title_attachments_incomplete, Snackbar.LENGTH_INDEFINITE)
                        .setGestureInsetBottomIgnored(true);
                snackbar.setAction(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        snackbar.dismiss();
                    }
                });
                snackbar.show();
            }

            DB db = DB.getInstance(getContext());

            db.attachment().liveAttachments(data.draft.id).observe(getViewLifecycleOwner(),
                    new Observer<List<EntityAttachment>>() {
                        private Integer count = null;

                        @Override
                        public void onChanged(@Nullable List<EntityAttachment> attachments) {
                            if (attachments == null)
                                attachments = new ArrayList<>();

                            List<EntityAttachment> a = new ArrayList<>(attachments);
                            rvAttachment.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if (adapter != null)
                                            adapter.set(a);
                                    } catch (Throwable ex) {
                                        Log.e(ex);
                                        /*
                                            java.lang.IllegalStateException: Cannot call this method while RecyclerView is computing a layout or scrolling androidx.recyclerview.widget.RecyclerView{f9baa84 VFED..... ........ 0,245-720,1445 #7f0a03fd app:id/rvAttachment}, adapter:eu.faircode.email.AdapterAttachment@954026d, layout:androidx.recyclerview.widget.LinearLayoutManager@ed06ea2, context:eu.faircode.email.ActivityCompose@d14c627
                                              at androidx.recyclerview.widget.RecyclerView.assertNotInLayoutOrScroll(SourceFile:3)
                                              at androidx.recyclerview.widget.RecyclerView$RecyclerViewDataObserver.onItemRangeChanged(SourceFile:1)
                                              at androidx.recyclerview.widget.RecyclerView$AdapterDataObservable.notifyItemRangeChanged(SourceFile:2)
                                              at androidx.recyclerview.widget.RecyclerView$Adapter.notifyItemRangeChanged(SourceFile:3)
                                              at androidx.recyclerview.widget.AdapterListUpdateCallback.onChanged(SourceFile:1)
                                              at androidx.recyclerview.widget.BatchingListUpdateCallback.dispatchLastEvent(SourceFile:2)
                                              at androidx.recyclerview.widget.DiffUtil$DiffResult.dispatchUpdatesTo(SourceFile:36)
                                              at eu.faircode.email.AdapterAttachment.set(SourceFile:6)
                                              at eu.faircode.email.FragmentCompose$38$3.onChanged(SourceFile:3)
                                              at eu.faircode.email.FragmentCompose$38$3.onChanged(SourceFile:1)
                                              at androidx.lifecycle.LiveData.considerNotify(SourceFile:6)
                                              at androidx.lifecycle.LiveData.dispatchingValue(SourceFile:8)
                                              at androidx.lifecycle.LiveData.setValue(SourceFile:4)
                                              at androidx.lifecycle.LiveData$1.run(SourceFile:5)
                                              at android.os.Handler.handleCallback(Handler.java:751)
                                         */
                                    }
                                }
                            });

                            ibRemoveAttachments.setVisibility(attachments.size() > 2 ? View.VISIBLE : View.GONE);
                            grpAttachments.setVisibility(attachments.size() > 0 ? View.VISIBLE : View.GONE);

                            boolean downloading = false;
                            for (EntityAttachment attachment : attachments) {
                                if (attachment.isEncryption())
                                    continue;
                                if (attachment.progress != null)
                                    downloading = true;
                            }

                            Log.i("Attachments=" + attachments.size() + " downloading=" + downloading);

                            rvAttachment.setTag(downloading);
                            checkInternet();

                            if (count != null && count > attachments.size()) {
                                boolean updated = false;
                                Editable edit = etBody.getEditableText();

                                ImageSpan[] spans = edit.getSpans(0, edit.length(), ImageSpan.class);
                                for (int i = 0; i < spans.length && !updated; i++) {
                                    ImageSpan span = spans[i];
                                    String source = span.getSource();
                                    if (source != null && source.startsWith("cid:")) {
                                        String cid = "<" + source.substring(4) + ">";
                                        boolean found = false;
                                        for (EntityAttachment attachment : attachments)
                                            if (cid.equals(attachment.cid)) {
                                                found = true;
                                                break;
                                            }

                                        if (!found) {
                                            updated = true;
                                            int start = edit.getSpanStart(span);
                                            int end = edit.getSpanEnd(span);
                                            edit.removeSpan(span);
                                            edit.delete(start, end);
                                        }
                                    }
                                }

                                if (updated)
                                    etBody.setText(edit);
                            }

                            count = attachments.size();
                        }
                    });

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            final boolean plain_only = prefs.getBoolean("plain_only", false);

            db.message().liveMessage(data.draft.id).observe(getViewLifecycleOwner(), new Observer<EntityMessage>() {
                @Override
                public void onChanged(EntityMessage draft) {
                    // Draft was deleted
                    if (draft == null || draft.ui_hide)
                        finish();
                    else {
                        encrypt = draft.ui_encrypt;
                        invalidateOptionsMenu();

                        Log.i("Draft content=" + draft.content);
                        if (draft.content && state == State.NONE) {
                            Runnable postShow = null;
                            if (args.containsKey("images")) {
                                ArrayList<Uri> images = args.getParcelableArrayList("images");
                                args.remove("images"); // once

                                postShow = new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            boolean image_dialog = prefs.getBoolean("image_dialog", true);
                                            if (image_dialog) {
                                                Helper.hideKeyboard(view);

                                                Bundle aargs = new Bundle();
                                                aargs.putInt("title", android.R.string.ok);
                                                aargs.putParcelableArrayList("images", images);

                                                FragmentDialogAddImage fragment = new FragmentDialogAddImage();
                                                fragment.setArguments(aargs);
                                                fragment.setTargetFragment(FragmentCompose.this, REQUEST_SHARED);
                                                fragment.show(getParentFragmentManager(), "compose:shared");
                                            } else
                                                onAddImageFile(images);
                                        } catch (Throwable ex) {
                                            Log.e(ex);
                                        }
                                    }
                                };
                            }

                            showDraft(draft, false, postShow, args.getInt("selection"));
                        }

                        tvDsn.setVisibility(
                                draft.dsn != null && !EntityMessage.DSN_NONE.equals(draft.dsn)
                                        ? View.VISIBLE : View.GONE);

                        tvResend.setVisibility(
                                draft.headers != null && Boolean.TRUE.equals(draft.resend)
                                        ? View.VISIBLE : View.GONE);

                        tvPlainTextOnly.setVisibility(
                                draft.isPlainOnly() && !plain_only
                                        ? View.VISIBLE : View.GONE);

                        tvNoInternet.setTag(draft.content);
                        checkInternet();
                    }
                }
            });

            boolean threading = prefs.getBoolean("threading", true);
            if (threading)
                db.message().liveUnreadThread(data.draft.account, data.draft.thread).observe(getViewLifecycleOwner(), new Observer<List<EntityMessage>>() {
                    private int lastDiff = 0;
                    private List<EntityMessage> base = null;

                    @Override
                    public void onChanged(List<EntityMessage> messages) {
                        if (messages == null)
                            return;

                        if (base == null) {
                            base = messages;
                            return;
                        }

                        int diff = (messages.size() - base.size());
                        if (diff > lastDiff) {
                            lastDiff = diff;
                            String msg = getResources().getQuantityString(
                                    R.plurals.title_notification_unseen, diff, diff);

                            Snackbar snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_INDEFINITE)
                                    .setGestureInsetBottomIgnored(true);
                            snackbar.setAction(R.string.title_show, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    EntityMessage message = messages.get(0);
                                    boolean notify_remove = prefs.getBoolean("notify_remove", true);

                                    Intent thread = new Intent(v.getContext(), ActivityView.class);
                                    thread.setAction("thread:" + message.id);
                                    thread.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    thread.putExtra("account", message.account);
                                    thread.putExtra("folder", message.folder);
                                    thread.putExtra("thread", message.thread);
                                    thread.putExtra("filter_archive", true);
                                    thread.putExtra("ignore", notify_remove);

                                    v.getContext().startActivity(thread);
                                    getActivity().finish();
                                }
                            });
                            snackbar.show();
                        }
                    }
                });
        }

        @Override
        protected void onException(Bundle args, Throwable ex) {
            pbWait.setVisibility(View.GONE);

            if (ex instanceof MessageRemovedException)
                finish();
            else if (ex instanceof OperationCanceledException) {
                Snackbar snackbar = Snackbar.make(view, ex.getMessage(), Snackbar.LENGTH_INDEFINITE)
                        .setGestureInsetBottomIgnored(true);
                snackbar.setAction(R.string.title_fix, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        v.getContext().startActivity(new Intent(v.getContext(), ActivitySetup.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                .putExtra("manual", true));
                        getActivity().finish();
                    }
                });
                snackbar.show();
            } else
                handleException(ex);
        }
    }.setExecutor(executor);

    private void handleException(Throwable ex) {
        // External app sending absolute file
        if (ex instanceof NoStreamException)
            ((NoStreamException) ex).report(getActivity());
        else if (ex instanceof FileNotFoundException ||
                ex instanceof IllegalArgumentException ||
                ex instanceof IllegalStateException) {
                    /*
                        java.lang.IllegalStateException: Failed to mount
                          at android.os.Parcel.createException(Parcel.java:2079)
                          at android.os.Parcel.readException(Parcel.java:2039)
                          at android.database.DatabaseUtils.readExceptionFromParcel(DatabaseUtils.java:188)
                          at android.database.DatabaseUtils.readExceptionWithFileNotFoundExceptionFromParcel(DatabaseUtils.java:151)
                          at android.content.ContentProviderProxy.openTypedAssetFile(ContentProviderNative.java:705)
                          at android.content.ContentResolver.openTypedAssetFileDescriptor(ContentResolver.java:1687)
                          at android.content.ContentResolver.openAssetFileDescriptor(ContentResolver.java:1503)
                          at android.content.ContentResolver.openInputStream(ContentResolver.java:1187)
                          at eu.faircode.email.FragmentCompose.addAttachment(SourceFile:27)
                     */
            Snackbar.make(view, ex.toString(), Snackbar.LENGTH_LONG)
                    .setGestureInsetBottomIgnored(true).show();
        } else {
            if (ex instanceof IOException &&
                    ex.getCause() instanceof ErrnoException &&
                    ((ErrnoException) ex.getCause()).errno == ENOSPC)
                ex = new IOException(getContext().getString(R.string.app_cake), ex);

            // External app didn't grant URI permissions
            if (ex instanceof SecurityException)
                ex = new Throwable(getString(R.string.title_no_permissions), ex);

            Log.unexpectedError(getParentFragmentManager(), ex,
                    !(ex instanceof IOException || ex.getCause() instanceof IOException));
                    /*
                        java.lang.IllegalStateException: java.io.IOException: Failed to redact /storage/emulated/0/Download/97203830-piston-vecteur-icône-simple-symbole-plat-sur-fond-blanc.jpg
                          at android.os.Parcel.createExceptionOrNull(Parcel.java:2381)
                          at android.os.Parcel.createException(Parcel.java:2357)
                          at android.os.Parcel.readException(Parcel.java:2340)
                          at android.database.DatabaseUtils.readExceptionFromParcel(DatabaseUtils.java:190)
                          at android.database.DatabaseUtils.readExceptionWithFileNotFoundExceptionFromParcel(DatabaseUtils.java:153)
                          at android.content.ContentProviderProxy.openTypedAssetFile(ContentProviderNative.java:804)
                          at android.content.ContentResolver.openTypedAssetFileDescriptor(ContentResolver.java:2002)
                          at android.content.ContentResolver.openAssetFileDescriptor(ContentResolver.java:1817)
                          at android.content.ContentResolver.openInputStream(ContentResolver.java:1494)
                          at eu.faircode.email.FragmentCompose.addAttachment(SourceFile:27)
                     */
        }
    }

    private SimpleTask<EntityMessage> actionLoader = new SimpleTask<EntityMessage>() {
        @Override
        protected void onPreExecute(Bundle args) {
            if (args.getBundle("extras").getBoolean("silent"))
                return;

            setBusy(true);
        }

        @Override
        protected void onPostExecute(Bundle args) {
            if (args.getBundle("extras").getBoolean("silent"))
                return;

            int action = args.getInt("action");
            boolean needsEncryption = args.getBoolean("needsEncryption");
            if (action != R.id.action_check || needsEncryption)
                setBusy(false);
        }

        @Override
        protected EntityMessage onExecute(final Context context, Bundle args) throws Throwable {
            // Get data
            long id = args.getLong("id");
            int action = args.getInt("action");
            long aid = args.getLong("account");
            long iid = args.getLong("identity");
            String extra = args.getString("extra");
            String to = args.getString("to");
            String cc = args.getString("cc");
            String bcc = args.getString("bcc");
            String subject = args.getString("subject");
            Spanned loaded = (Spanned) args.getCharSequence("loaded");
            Spanned spanned = (Spanned) args.getCharSequence("spanned");
            boolean signature = args.getBoolean("signature");
            boolean empty = args.getBoolean("empty");
            boolean notext = args.getBoolean("notext");
            Bundle extras = args.getBundle("extras");

            boolean silent = extras.getBoolean("silent");

            boolean dirty = false;
            String body = HtmlHelper.toHtml(spanned, context);
            EntityMessage draft;

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean discard_delete = prefs.getBoolean("discard_delete", true);
            boolean write_below = prefs.getBoolean("write_below", false);
            boolean save_drafts = prefs.getBoolean("save_drafts", true);
            int send_delayed = prefs.getInt("send_delayed", 0);

            DB db = DB.getInstance(context);
            try {
                db.beginTransaction();

                // Get draft & selected identity
                draft = db.message().getMessage(id);
                EntityIdentity identity = db.identity().getIdentity(iid);

                // Draft deleted by server
                if (draft == null || draft.ui_hide)
                    throw new MessageRemovedException("Draft for action was deleted hide=" + (draft != null));

                Log.i("Load action id=" + draft.id + " action=" + getActionName(action));

                if (action == R.id.action_delete) {
                    dirty = true;
                    EntityFolder trash = db.folder().getFolderByType(draft.account, EntityFolder.TRASH);
                    EntityFolder drafts = db.folder().getFolderByType(draft.account, EntityFolder.DRAFTS);
                    if (empty || trash == null || discard_delete || (drafts != null && drafts.local))
                        EntityOperation.queue(context, draft, EntityOperation.DELETE);
                    else {
                        Map<String, String> c = new HashMap<>();
                        c.put("id", draft.id == null ? null : Long.toString(draft.id));
                        c.put("encrypt", draft.encrypt + "/" + draft.ui_encrypt);
                        Log.breadcrumb("Discard draft", c);

                        EntityOperation.queue(context, draft, EntityOperation.ADD);
                        EntityOperation.queue(context, draft, EntityOperation.MOVE, trash.id);
                    }

                    getMainHandler().post(new Runnable() {
                        public void run() {
                            ToastEx.makeText(context, R.string.title_draft_deleted, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    // Move draft to new account
                    if (draft.account != aid && aid >= 0) {
                        Log.i("Account changed");

                        Long uid = draft.uid;
                        String msgid = draft.msgid;
                        boolean content = draft.content;
                        Boolean ui_hide = draft.ui_hide;

                        // To prevent violating constraints
                        draft.uid = null;
                        draft.msgid = null;
                        db.message().updateMessage(draft);

                        // Create copy to delete
                        draft.id = null;
                        draft.uid = uid;
                        draft.msgid = msgid;
                        draft.content = false;
                        draft.ui_hide = true;
                        draft.id = db.message().insertMessage(draft);
                        EntityOperation.queue(context, draft, EntityOperation.DELETE);

                        // Restore original with new account, no uid and new msgid
                        draft.id = id;
                        draft.account = aid;
                        draft.folder = db.folder().getFolderByType(aid, EntityFolder.DRAFTS).id;
                        draft.uid = null;
                        draft.msgid = EntityMessage.generateMessageId();
                        draft.content = content;
                        draft.ui_hide = ui_hide;
                        db.message().updateMessage(draft);

                        if (draft.content)
                            dirty = true;
                    }

                    Map<String, String> crumb = new HashMap<>();
                    crumb.put("draft", draft.folder + ":" + draft.id);
                    crumb.put("content", Boolean.toString(draft.content));
                    crumb.put("revision", Integer.toString(draft.revision == null ? -1 : draft.revision));
                    crumb.put("revisions", Integer.toString(draft.revisions == null ? -1 : draft.revisions));
                    crumb.put("file", Boolean.toString(draft.getFile(context).exists()));
                    crumb.put("action", getActionName(action));
                    Log.breadcrumb("compose", crumb);

                    List<EntityAttachment> attachments = db.attachment().getAttachments(draft.id);

                    // Get data
                    InternetAddress[] afrom = (identity == null ? null : new InternetAddress[]{new InternetAddress(identity.email, identity.name, StandardCharsets.UTF_8.name())});
                    InternetAddress[] ato = MessageHelper.dedup(MessageHelper.parseAddresses(context, to));
                    InternetAddress[] acc = MessageHelper.dedup(MessageHelper.parseAddresses(context, cc));
                    InternetAddress[] abcc = MessageHelper.dedup(MessageHelper.parseAddresses(context, bcc));

                    // Safe guard
                    if (action == R.id.action_send) {
                        checkAddress(ato, context);
                        checkAddress(acc, context);
                        checkAddress(abcc, context);
                    }

                    if (TextUtils.isEmpty(extra))
                        extra = null;

                    List<Integer> eparts = new ArrayList<>();
                    for (EntityAttachment attachment : attachments)
                        if (attachment.available)
                            if (attachment.isEncryption())
                                eparts.add(attachment.encryption);

                    if (EntityMessage.PGP_SIGNONLY.equals(draft.ui_encrypt)) {
                        if (!eparts.contains(EntityAttachment.PGP_KEY) ||
                                !eparts.contains(EntityAttachment.PGP_SIGNATURE) ||
                                !eparts.contains(EntityAttachment.PGP_CONTENT))
                            dirty = true;
                    } else if (EntityMessage.PGP_SIGNENCRYPT.equals(draft.ui_encrypt)) {
                        if (!eparts.contains(EntityAttachment.PGP_KEY) ||
                                !eparts.contains(EntityAttachment.PGP_MESSAGE))
                            dirty = true;
                    } else if (EntityMessage.SMIME_SIGNONLY.equals(draft.ui_encrypt)) {
                        if (!eparts.contains(EntityAttachment.SMIME_SIGNATURE) ||
                                !eparts.contains(EntityAttachment.SMIME_CONTENT))
                            dirty = true;
                    } else if (EntityMessage.SMIME_SIGNENCRYPT.equals(draft.ui_encrypt)) {
                        if (!eparts.contains(EntityAttachment.SMIME_MESSAGE))
                            dirty = true;
                    }

                    Long ident = (identity == null ? null : identity.id);
                    if (!Objects.equals(draft.identity, ident) ||
                            !Objects.equals(draft.extra, extra) ||
                            !MessageHelper.equal(draft.from, afrom) ||
                            !MessageHelper.equal(draft.to, ato) ||
                            !MessageHelper.equal(draft.cc, acc) ||
                            !MessageHelper.equal(draft.bcc, abcc) ||
                            !Objects.equals(draft.subject, subject) ||
                            !draft.signature.equals(signature) ||
                            !Objects.equals(last_plain_only, draft.plain_only) ||
                            !EntityAttachment.equals(last_attachments, attachments))
                        dirty = true;

                    last_plain_only = draft.plain_only;
                    last_attachments = attachments;

                    if (dirty) {
                        // Update draft
                        draft.identity = ident;
                        draft.extra = extra;
                        draft.from = afrom;
                        draft.to = ato;
                        draft.cc = acc;
                        draft.bcc = abcc;
                        draft.subject = subject;
                        draft.signature = signature;
                        draft.sender = MessageHelper.getSortKey(draft.from);
                        Uri lookupUri = ContactInfo.getLookupUri(draft.from);
                        draft.avatar = (lookupUri == null ? null : lookupUri.toString());
                        db.message().updateMessage(draft);
                    }

                    Document doc = JsoupEx.parse(draft.getFile(context));
                    Element first = (doc.body().childrenSize() == 0 ? null : doc.body().child(0));
                    boolean below = (first != null && first.attr("fairemail").equals("reference"));
                    doc.select("div[fairemail=signature]").remove();
                    Elements ref = doc.select("div[fairemail=reference]");
                    ref.remove();

                    if (extras.containsKey("html"))
                        dirty = true;

                    if (below != write_below &&
                            doc.body().childrenSize() > 0 &&
                            draft.wasforwardedfrom == null)
                        dirty = true;

                    if (!dirty)
                        if (loaded == null) {
                            Document b = JsoupEx.parse(body); // Is-dirty
                            if (!Objects.equals(b.body().html(), doc.body().html()))
                                dirty = true;
                        } else {
                            // Was not dirty before
                            String hloaded = HtmlHelper.toHtml(loaded, context);
                            String hspanned = HtmlHelper.toHtml(spanned, context);
                            if (!Objects.equals(hloaded, hspanned))
                                dirty = true;
                        }

                    if (draft.revision == null) {
                        draft.revision = 1;
                        draft.revisions = 1;
                    }

                    int revision = draft.revision; // Save for undo/redo
                    if (dirty) {
                        dirty = true;

                        // Get saved body
                        Document d;
                        if (extras.containsKey("html")) {
                            // Save current revision
                            Document c = JsoupEx.parse(body);

                            for (Element e : ref)
                                if (write_below && draft.wasforwardedfrom == null)
                                    c.body().prependChild(e);
                                else
                                    c.body().appendChild(e);

                            addSignature(context, c, draft, identity);

                            Helper.writeText(draft.getFile(context, draft.revision), c.html());

                            d = JsoupEx.parse(extras.getString("html"));
                        } else {
                            d = JsoupEx.parse(body); // Save

                            for (Element e : ref)
                                if (write_below && draft.wasforwardedfrom == null)
                                    d.body().prependChild(e);
                                else
                                    d.body().appendChild(e);

                            addSignature(context, d, draft, identity);
                        }

                        body = d.html();

                        // Create new revision
                        draft.revisions++;
                        draft.revision = draft.revisions;

                        Helper.writeText(draft.getFile(context, draft.revision), body);
                    } else
                        body = Helper.readText(draft.getFile(context));

                    if (action == R.id.action_undo || action == R.id.action_redo) {
                        if (action == R.id.action_undo) {
                            if (revision > 1)
                                draft.revision = revision - 1;
                            else
                                draft.revision = revision;
                        } else {
                            if (revision < draft.revisions)
                                draft.revision = revision + 1;
                            else
                                draft.revision = revision;
                        }

                        // Restore revision
                        Log.i("Restoring revision=" + draft.revision);
                        File file = draft.getFile(context, draft.revision);
                        if (file.exists())
                            body = Helper.readText(file);
                        else
                            Log.e("Missing" +
                                    " revision=" + draft.revision + "/" + draft.revisions +
                                    " action=" + getActionName(action));

                        dirty = true;
                    } else if (action == R.id.action_send) {
                        if (!draft.isPlainOnly()) {
                            // Remove unused inline images
                            List<String> cids = new ArrayList<>();
                            Document d = JsoupEx.parse(body);
                            for (Element element : d.select("img")) {
                                String src = element.attr("src");
                                if (src.startsWith("cid:"))
                                    cids.add("<" + src.substring(4) + ">");
                            }

                            for (EntityAttachment attachment : new ArrayList<>(attachments))
                                if (attachment.isInline() && attachment.isImage() &&
                                        attachment.cid != null && !cids.contains(attachment.cid)) {
                                    Log.i("Removing unused inline attachment cid=" + attachment.cid);
                                    attachments.remove(attachment);
                                    db.attachment().deleteAttachment(attachment.id);
                                    dirty = true;
                                }
                        } else {
                            // Convert inline images to attachments
                            for (EntityAttachment attachment : new ArrayList<>(attachments))
                                if (attachment.isInline() && attachment.isImage()) {
                                    Log.i("Converting to attachment cid=" + attachment.cid);
                                    attachment.disposition = Part.ATTACHMENT;
                                    attachment.cid = null;
                                    db.attachment().setDisposition(attachment.id, attachment.disposition, attachment.cid);
                                    dirty = true;
                                }
                        }
                    }

                    Helper.writeText(draft.getFile(context), body);

                    String full = HtmlHelper.getFullText(body);
                    draft.preview = HtmlHelper.getPreview(full);
                    draft.language = HtmlHelper.getLanguage(context, draft.subject, full);
                    db.message().setMessageContent(draft.id,
                            true,
                            draft.language,
                            draft.plain_only, // unchanged
                            draft.preview,
                            null);

                    db.message().setMessageRevision(draft.id, draft.revision);
                    db.message().setMessageRevisions(draft.id, draft.revisions);

                    if (dirty) {
                        draft.received = new Date().getTime();
                        draft.sent = draft.received;
                        db.message().setMessageReceived(draft.id, draft.received);
                        db.message().setMessageSent(draft.id, draft.sent);
                    }

                    if (silent) {
                        // Skip storing on the server, etc
                        db.setTransactionSuccessful();
                        return draft;
                    }

                    // Execute action
                    boolean encrypted = extras.getBoolean("encrypted");
                    boolean shouldEncrypt = EntityMessage.PGP_SIGNENCRYPT.equals(draft.ui_encrypt) ||
                            (EntityMessage.PGP_SIGNONLY.equals(draft.ui_encrypt) && action == R.id.action_send) ||
                            EntityMessage.SMIME_SIGNENCRYPT.equals(draft.ui_encrypt) ||
                            (EntityMessage.SMIME_SIGNONLY.equals(draft.ui_encrypt) && action == R.id.action_send);
                    boolean needsEncryption = (dirty && !encrypted && shouldEncrypt);
                    boolean autosave = extras.getBoolean("autosave");
                    if (needsEncryption && !autosave) {
                        args.putBoolean("needsEncryption", true);
                        db.setTransactionSuccessful();
                        return draft;
                    }

                    if (!shouldEncrypt && !autosave)
                        for (EntityAttachment attachment : attachments)
                            if (attachment.isEncryption())
                                db.attachment().deleteAttachment(attachment.id);

                    if (action == R.id.action_save ||
                            action == R.id.action_undo ||
                            action == R.id.action_redo ||
                            action == R.id.action_check) {
                        boolean unencrypted =
                                (!EntityMessage.PGP_SIGNENCRYPT.equals(draft.ui_encrypt) &&
                                        !EntityMessage.SMIME_SIGNENCRYPT.equals(draft.ui_encrypt));
                        if ((dirty && unencrypted) || encrypted) {
                            if (save_drafts) {
                                Map<String, String> c = new HashMap<>();
                                c.put("id", draft.id == null ? null : Long.toString(draft.id));
                                c.put("dirty", Boolean.toString(dirty));
                                c.put("encrypt", draft.encrypt + "/" + draft.ui_encrypt);
                                c.put("encrypted", Boolean.toString(encrypted));
                                c.put("needsEncryption", Boolean.toString(needsEncryption));
                                c.put("autosave", Boolean.toString(autosave));
                                Log.breadcrumb("Save draft", c);

                                EntityOperation.queue(context, draft, EntityOperation.ADD);
                            }
                        }

                        if (action == R.id.action_check) {
                            // Check data
                            if (draft.identity == null)
                                throw new IllegalArgumentException(context.getString(R.string.title_from_missing));

                            if (false) {
                                EntityAccount account = db.account().getAccount(draft.account);
                                EntityFolder sent = db.folder().getFolderByType(draft.account, EntityFolder.SENT);
                                if (account != null && account.protocol == EntityAccount.TYPE_IMAP && sent == null)
                                    args.putBoolean("sent_missing", true);
                            }

                            try {
                                checkAddress(ato, context);
                                checkAddress(acc, context);
                                checkAddress(abcc, context);

                                List<InternetAddress> check = new ArrayList<>();
                                List<String> checked = new ArrayList<>();
                                List<String> dup = new ArrayList<>();
                                if (ato != null)
                                    check.addAll(Arrays.asList(ato));
                                if (acc != null)
                                    check.addAll(Arrays.asList(acc));
                                if (abcc != null)
                                    check.addAll(Arrays.asList(abcc));

                                for (InternetAddress a : check) {
                                    String email = a.getAddress();
                                    if (TextUtils.isEmpty(email))
                                        continue;
                                    if (checked.contains(a.getAddress()))
                                        dup.add(email);
                                    else
                                        checked.add(email);
                                }

                                if (dup.size() > 0)
                                    throw new AddressException(context.getString(
                                            R.string.title_address_duplicate,
                                            TextUtils.join(", ", dup)));
                            } catch (AddressException ex) {
                                args.putString("address_error", ex.getMessage());
                            }

                            try {
                                checkMx(ato, context);
                                checkMx(acc, context);
                                checkMx(abcc, context);
                            } catch (UnknownHostException ex) {
                                args.putString("mx_error", ex.getMessage());
                            }

                            if (draft.to == null && draft.cc == null && draft.bcc == null &&
                                    (identity == null || (identity.cc == null && identity.bcc == null)))
                                args.putBoolean("remind_to", true);

                            //if (TextUtils.isEmpty(draft.extra) &&
                            //        identity != null && identity.sender_extra)
                            //    args.putBoolean("remind_extra", true);

                            List<Address> recipients = new ArrayList<>();
                            if (draft.to != null)
                                recipients.addAll(Arrays.asList(draft.to));
                            if (draft.cc != null)
                                recipients.addAll(Arrays.asList(draft.cc));
                            if (draft.bcc != null)
                                recipients.addAll(Arrays.asList(draft.bcc));

                            boolean noreply = false;
                            for (Address recipient : recipients)
                                if (MessageHelper.isNoReply(recipient)) {
                                    noreply = true;
                                    break;
                                }
                            args.putBoolean("remind_noreply", noreply);

                            if (identity != null && !TextUtils.isEmpty(identity.internal)) {
                                boolean external = false;
                                String[] internals = identity.internal.split(",");
                                for (Address recipient : recipients) {
                                    String email = ((InternetAddress) recipient).getAddress();
                                    String domain = UriHelper.getEmailDomain(email);
                                    if (domain == null)
                                        continue;

                                    boolean found = false;
                                    for (String internal : internals)
                                        if (internal.equalsIgnoreCase(domain)) {
                                            found = true;
                                            break;
                                        }
                                    if (!found) {
                                        external = true;
                                        break;
                                    }
                                }
                                args.putBoolean("remind_external", external);
                            }

                            if ((draft.dsn == null ||
                                    EntityMessage.DSN_NONE.equals(draft.dsn)) &&
                                    (draft.ui_encrypt == null ||
                                            EntityMessage.ENCRYPT_NONE.equals(draft.ui_encrypt))) {
                                args.putBoolean("remind_pgp", PgpHelper.hasPgpKey(context, recipients));
                                args.putBoolean("remind_smime", SmimeHelper.hasSmimeKey(context, recipients));
                            }

                            if (TextUtils.isEmpty(draft.subject))
                                args.putBoolean("remind_subject", true);

                            Document d = JsoupEx.parse(body);

                            if (notext &&
                                    d.select("div[fairemail=reference]").isEmpty())
                                args.putBoolean("remind_text", true);

                            boolean styled = HtmlHelper.isStyled(d);
                            args.putBoolean("styled", styled);

                            int attached = 0;
                            for (EntityAttachment attachment : attachments)
                                if (!attachment.available)
                                    throw new IllegalArgumentException(context.getString(R.string.title_attachments_missing));
                                else if (attachment.isAttachment())
                                    attached++;

                            // Check for missing attachments
                            if (attached == 0) {
                                List<String> keywords = new ArrayList<>();
                                for (String text : Helper.getStrings(context, R.string.title_attachment_keywords))
                                    keywords.addAll(Arrays.asList(text.split(",")));

                                d.select("div[fairemail=signature]").remove();
                                d.select("div[fairemail=reference]").remove();

                                String text = d.text();
                                for (String keyword : keywords)
                                    if (text.matches("(?si).*\\b" + Pattern.quote(keyword.trim()) + "\\b.*")) {
                                        args.putBoolean("remind_attachment", true);
                                        break;
                                    }
                            }

                            if (EntityMessage.DSN_HARD_BOUNCE.equals(draft.dsn))
                                args.putBoolean("remind_dsn", true);

                            // Check size
                            if (identity != null && identity.max_size != null)
                                try {
                                    Properties props = MessageHelper.getSessionProperties(true);
                                    if (identity.unicode)
                                        props.put("mail.mime.allowutf8", "true");
                                    Session isession = Session.getInstance(props, null);
                                    Message imessage = MessageHelper.from(context, draft, identity, isession, false);

                                    File file = draft.getRawFile(context);
                                    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
                                        imessage.writeTo(os);
                                    }

                                    long size = file.length();
                                    if (size > identity.max_size) {
                                        args.putBoolean("remind_size", true);
                                        args.putLong("size", size);
                                        args.putLong("max_size", identity.max_size);
                                    }
                                } catch (Throwable ex) {
                                    Log.e(ex);
                                }
                        } else {
                            int mid;
                            if (action == R.id.action_undo)
                                mid = R.string.title_undo;
                            else if (action == R.id.action_redo)
                                mid = R.string.title_redo;
                            else
                                mid = R.string.title_draft_saved;
                            final String msg = context.getString(mid) +
                                    (BuildConfig.DEBUG
                                            ? " " + draft.revision + (dirty ? "*" : "")
                                            : "");

                            getMainHandler().post(new Runnable() {
                                public void run() {
                                    ToastEx.makeText(context, msg, Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                    } else if (action == R.id.action_send) {
                        // Delete draft (cannot move to outbox)
                        EntityOperation.queue(context, draft, EntityOperation.DELETE);

                        EntityFolder outbox = db.folder().getOutbox();
                        if (outbox == null) {
                            Log.w("Outbox missing");
                            outbox = EntityFolder.getOutbox();
                            outbox.id = db.folder().insertFolder(outbox);
                        }

                        // Delay sending message
                        if (draft.ui_snoozed == null && send_delayed != 0) {
                            if (extras.getBoolean("now"))
                                draft.ui_snoozed = null;
                            else
                                draft.ui_snoozed = new Date().getTime() + send_delayed * 1000L;
                        }

                        if (draft.ui_snoozed != null)
                            draft.received = draft.ui_snoozed;

                        // Copy message to outbox
                        draft.id = null;
                        draft.folder = outbox.id;
                        draft.uid = null;
                        draft.fts = false;
                        draft.ui_hide = false;
                        draft.id = db.message().insertMessage(draft);
                        Helper.writeText(draft.getFile(context), body);

                        // Move attachments
                        for (EntityAttachment attachment : attachments)
                            db.attachment().setMessage(attachment.id, draft.id);

                        // Send message
                        if (draft.ui_snoozed == null)
                            EntityOperation.queue(context, draft, EntityOperation.SEND);

                        final String feedback;
                        if (draft.ui_snoozed == null)
                            feedback = context.getString(R.string.title_queued);
                        else {
                            DateFormat DTF = Helper.getDateTimeInstance(context);
                            feedback = context.getString(R.string.title_queued_at, DTF.format(draft.ui_snoozed));
                        }

                        getMainHandler().post(new Runnable() {
                            public void run() {
                                ToastEx.makeText(context, feedback, Toast.LENGTH_LONG).show();
                            }
                        });

                        if (extras.getBoolean("archive")) {
                            EntityFolder archive = db.folder().getFolderByType(draft.account, EntityFolder.ARCHIVE);
                            if (archive != null) {
                                List<EntityMessage> messages = db.message().getMessagesByMsgId(draft.account, draft.inreplyto);
                                if (messages != null)
                                    for (EntityMessage message : messages)
                                        EntityOperation.queue(context, message, EntityOperation.MOVE, archive.id);
                            }
                        }
                    }
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            args.putBoolean("dirty", dirty);
            if (dirty)
                ServiceSynchronize.eval(context, "compose/action");

            if (action == R.id.action_send)
                if (draft.ui_snoozed == null)
                    ServiceSend.start(context);
                else {
                    Log.i("Delayed send id=" + draft.id + " at " + new Date(draft.ui_snoozed));
                    EntityMessage.snooze(context, draft.id, draft.ui_snoozed);
                }

            return draft;
        }

        @Override
        protected void onExecuted(Bundle args, EntityMessage draft) {
            if (draft == null)
                return;

            bottom_navigation.getMenu().findItem(R.id.action_undo).setVisible(draft.revision > 1);
            bottom_navigation.getMenu().findItem(R.id.action_redo).setVisible(draft.revision < draft.revisions);

            if (args.getBundle("extras").getBoolean("silent")) {
                etBody.setTag(null);
                return;
            }

            boolean needsEncryption = args.getBoolean("needsEncryption");
            int action = args.getInt("action");
            Log.i("Loaded action id=" + draft.id +
                    " action=" + getActionName(action) + " encryption=" + needsEncryption);

            int toPos = etTo.getSelectionStart();
            int ccPos = etCc.getSelectionStart();
            int bccPos = etBcc.getSelectionStart();

            etTo.setText(MessageHelper.formatAddressesCompose(draft.to));
            etCc.setText(MessageHelper.formatAddressesCompose(draft.cc));
            etBcc.setText(MessageHelper.formatAddressesCompose(draft.bcc));

            if (toPos >= 0 && toPos <= etTo.getText().length())
                etTo.setSelection(toPos);
            if (ccPos >= 0 && ccPos <= etCc.getText().length())
                etCc.setSelection(ccPos);
            if (bccPos >= 0 && bccPos <= etBcc.getText().length())
                etBcc.setSelection(bccPos);

            boolean dirty = args.getBoolean("dirty");
            if (dirty)
                etBody.setTag(null);

            Bundle extras = args.getBundle("extras");
            boolean show = extras.getBoolean("show");
            boolean refedit = extras.getBoolean("refedit");
            if (show)
                showDraft(draft, refedit, null, -1);

            if (needsEncryption) {
                if (ActivityBilling.isPro(getContext()) ||
                        EntityMessage.PGP_SIGNONLY.equals(draft.ui_encrypt) ||
                        EntityMessage.PGP_SIGNENCRYPT.equals(draft.ui_encrypt)) {
                    boolean interactive = args.getBoolean("interactive");
                    onEncrypt(draft, action, extras, interactive);
                } else
                    startActivity(new Intent(getContext(), ActivityBilling.class));
                return;
            }

            if (action == R.id.action_delete) {
                state = State.NONE;
                finish();

            } else if (action == R.id.action_undo || action == R.id.action_redo) {
                showDraft(draft, false, null, -1);

            } else if (action == R.id.action_save) {
                boolean autosave = extras.getBoolean("autosave");
                setFocus(
                        args.getInt("focus"),
                        args.getInt("start", -1),
                        args.getInt("end", -1),
                        args.getBoolean("ime") && !autosave);

            } else if (action == R.id.action_check) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                boolean send_dialog = prefs.getBoolean("send_dialog", true);
                boolean send_reminders = prefs.getBoolean("send_reminders", true);

                boolean force_dialog = extras.getBoolean("force_dialog", false);
                boolean sent_missing = args.getBoolean("sent_missing", false);
                String address_error = args.getString("address_error");
                String mx_error = args.getString("mx_error");
                boolean remind_dsn = args.getBoolean("remind_dsn", false);
                boolean remind_size = args.getBoolean("remind_size", false);
                boolean remind_pgp = args.getBoolean("remind_pgp", false);
                boolean remind_smime = args.getBoolean("remind_smime", false);
                boolean remind_to = args.getBoolean("remind_to", false);
                boolean remind_extra = args.getBoolean("remind_extra", false);
                boolean remind_noreply = args.getBoolean("remind_noreply", false);
                boolean remind_external = args.getBoolean("remind_external", false);
                boolean remind_subject = args.getBoolean("remind_subject", false);
                boolean remind_text = args.getBoolean("remind_text", false);
                boolean remind_attachment = args.getBoolean("remind_attachment", false);
                boolean styled = args.getBoolean("styled", false);

                int recipients = (draft.to == null ? 0 : draft.to.length) +
                        (draft.cc == null ? 0 : draft.cc.length) +
                        (draft.bcc == null ? 0 : draft.bcc.length);
                if (send_dialog || force_dialog ||
                        sent_missing || address_error != null || mx_error != null ||
                        remind_dsn || remind_size || remind_pgp || remind_smime ||
                        remind_to || remind_noreply || remind_external ||
                        recipients > RECIPIENTS_WARNING ||
                        (styled && draft.isPlainOnly()) ||
                        (send_reminders &&
                                (remind_extra || remind_subject || remind_text || remind_attachment))) {
                    setBusy(false);

                    Helper.hideKeyboard(view);

                    FragmentDialogSend fragment = new FragmentDialogSend();
                    fragment.setArguments(args);
                    fragment.setTargetFragment(FragmentCompose.this, REQUEST_SEND);
                    fragment.show(getParentFragmentManager(), "compose:send");
                } else
                    onAction(R.id.action_send, "dialog");

            } else if (action == R.id.action_send) {
                state = State.NONE;
                finish();
            }
        }

        @Override
        protected void onException(Bundle args, Throwable ex) {
            if (ex instanceof MessageRemovedException)
                finish();
            else {
                setBusy(false);
                if (ex instanceof IllegalArgumentException)
                    Snackbar.make(view, ex.getMessage(), Snackbar.LENGTH_LONG)
                            .setGestureInsetBottomIgnored(true).show();
                else
                    Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }

        private void checkAddress(InternetAddress[] addresses, Context context) throws AddressException {
            if (addresses == null)
                return;

            for (InternetAddress address : addresses)
                try {
                    address.validate();
                } catch (AddressException ex) {
                    throw new AddressException(context.getString(R.string.title_address_parse_error,
                            MessageHelper.formatAddressesCompose(new Address[]{address}), ex.getMessage()));
                }
        }

        private void checkMx(InternetAddress[] addresses, Context context) throws UnknownHostException {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean lookup_mx = prefs.getBoolean("lookup_mx", false);
            if (!lookup_mx)
                return;

            if (addresses == null)
                return;

            ConnectivityManager cm = Helper.getSystemService(context, ConnectivityManager.class);
            NetworkInfo ani = (cm == null ? null : cm.getActiveNetworkInfo());
            if (ani != null && ani.isConnected())
                DnsHelper.checkMx(context, addresses);
        }
    }.setExecutor(executor);

    private String getActionName(int id) {
        if (id == R.id.action_delete) {
            return "delete";
        } else if (id == R.id.action_undo) {
            return "undo";
        } else if (id == R.id.action_redo) {
            return "redo";
        } else if (id == R.id.action_save) {
            return "save";
        } else if (id == R.id.action_check) {
            return "check";
        } else if (id == R.id.action_send) {
            return "send";
        }
        return Integer.toString(id);
    }

    private void setBusy(boolean busy) {
        state = (busy ? State.LOADING : State.LOADED);
        Helper.setViewsEnabled(view, !busy);
        invalidateOptionsMenu();
    }

    private static void addSignature(Context context, Document document, EntityMessage draft, EntityIdentity identity) {
        if (!draft.signature ||
                identity == null || TextUtils.isEmpty(identity.signature))
            return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int signature_location = prefs.getInt("signature_location", 1);
        boolean usenet = prefs.getBoolean("usenet_signature", false);
        boolean write_below = prefs.getBoolean("write_below", false);

        Element div = document.createElement("div");
        div.attr("fairemail", "signature");

        if (usenet) {
            // https://datatracker.ietf.org/doc/html/rfc3676#section-4.3
            Element span = document.createElement("span");
            span.text("-- ");
            span.prependElement("br");
            span.appendElement("br");
            div.appendChild(span);
        }

        div.append(identity.signature);

        Elements ref = document.select("div[fairemail=reference]");
        if (signature_location == 0) // top
            document.body().prependChild(div);
        else if (ref.size() == 0 || signature_location == 2) // bottom
            document.body().appendChild(div);
        else if (signature_location == 1) // below text
            if (write_below && draft.wasforwardedfrom == null)
                document.body().appendChild(div);
            else
                ref.first().before(div);
    }

    private void showDraft(final EntityMessage draft, boolean refedit, Runnable postShow, int selection) {
        Bundle args = new Bundle();
        args.putLong("id", draft.id);
        args.putBoolean("show_images", show_images);

        new SimpleTask<Spanned[]>() {
            @Override
            protected void onPreExecute(Bundle args) {
                // Needed to get width for images
                grpBody.setVisibility(View.VISIBLE);
            }

            @Override
            protected void onPostExecute(Bundle args) {
                pbWait.setVisibility(View.GONE);
                media_bar.setVisibility(media ? View.VISIBLE : View.GONE);
                bottom_navigation.getMenu().findItem(R.id.action_undo).setVisible(draft.revision > 1);
                bottom_navigation.getMenu().findItem(R.id.action_redo).setVisible(draft.revision < draft.revisions);
                bottom_navigation.setVisibility(View.VISIBLE);

                Helper.setViewsEnabled(view, true);

                invalidateOptionsMenu();
            }

            @Override
            protected Spanned[] onExecute(final Context context, Bundle args) throws Throwable {
                final long id = args.getLong("id");
                final boolean show_images = args.getBoolean("show_images", false);

                int colorPrimary = Helper.resolveColor(context, R.attr.colorPrimary);
                final int colorBlockquote = Helper.resolveColor(context, R.attr.colorBlockquote, colorPrimary);
                int quoteGap = context.getResources().getDimensionPixelSize(R.dimen.quote_gap_size);
                int quoteStripe = context.getResources().getDimensionPixelSize(R.dimen.quote_stripe_width);

                DB db = DB.getInstance(context);
                EntityMessage draft = db.message().getMessage(id);
                if (draft == null || !draft.content)
                    throw new IllegalArgumentException(context.getString(R.string.title_no_body));

                Document doc = JsoupEx.parse(draft.getFile(context));
                doc.select("div[fairemail=signature]").remove();
                Elements ref = doc.select("div[fairemail=reference]");
                ref.remove();

                HtmlHelper.clearAnnotations(doc); // Legacy left-overs

                doc = HtmlHelper.sanitizeCompose(context, doc.html(), true);

                Spanned spannedBody = HtmlHelper.fromDocument(context, doc, new HtmlHelper.ImageGetterEx() {
                    @Override
                    public Drawable getDrawable(Element element) {
                        return ImageHelper.decodeImage(context,
                                id, element, true, zoom, 1.0f, etBody);
                    }
                }, null);

                SpannableStringBuilder bodyBuilder = new SpannableStringBuilderEx(spannedBody);
                QuoteSpan[] bodySpans = bodyBuilder.getSpans(0, bodyBuilder.length(), QuoteSpan.class);
                for (QuoteSpan quoteSpan : bodySpans) {
                    QuoteSpan q;
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
                        q = new QuoteSpan(colorBlockquote);
                    else
                        q = new QuoteSpan(colorBlockquote, quoteStripe, quoteGap);
                    bodyBuilder.setSpan(q,
                            bodyBuilder.getSpanStart(quoteSpan),
                            bodyBuilder.getSpanEnd(quoteSpan),
                            bodyBuilder.getSpanFlags(quoteSpan));
                    bodyBuilder.removeSpan(quoteSpan);
                }

                spannedBody = bodyBuilder;

                Spanned spannedRef = null;
                if (!ref.isEmpty()) {
                    Document dref = JsoupEx.parse(ref.outerHtml());
                    HtmlHelper.autoLink(dref);
                    Document quote = HtmlHelper.sanitizeView(context, dref, show_images);
                    spannedRef = HtmlHelper.fromDocument(context, quote,
                            new HtmlHelper.ImageGetterEx() {
                                @Override
                                public Drawable getDrawable(Element element) {
                                    return ImageHelper.decodeImage(context,
                                            id, element, show_images, zoom, 1.0f, tvReference);
                                }
                            },
                            null);

                    // Strip newline of reply header
                    if (spannedRef.length() > 0 && spannedRef.charAt(0) == '\n')
                        spannedRef = (Spanned) spannedRef.subSequence(1, spannedRef.length());

                    Locale ref_lang = TextHelper.detectLanguage(context, spannedRef.toString());
                    args.putSerializable("ref_lang", ref_lang);
                }

                args.putBoolean("ref_has_images", spannedRef != null &&
                        spannedRef.getSpans(0, spannedRef.length(), ImageSpan.class).length > 0);

                return new Spanned[]{spannedBody, spannedRef};
            }

            @Override
            protected void onExecuted(Bundle args, Spanned[] text) {
                etBody.setText(text[0]);
                etBody.setTag(text[0]);

                SpannableStringBuilder hint = new SpannableStringBuilderEx();
                hint.append(getString(R.string.title_body_hint));
                hint.append("\n");
                int start = hint.length();
                hint.append(getString(R.string.title_body_hint_style));
                hint.setSpan(new RelativeSizeSpan(0.7f), start, hint.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                etBody.setHint(hint);

                grpBody.setVisibility(View.VISIBLE);

                cbSignature.setChecked(draft.signature);
                tvSignature.setAlpha(draft.signature ? 1.0f : Helper.LOW_LIGHT);

                boolean ref_has_images = args.getBoolean("ref_has_images");

                Locale ref_lang = (Locale) args.getSerializable("ref_lang");
                if (ref_lang != null) {
                    String dl = ref_lang.getDisplayLanguage();
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        Locale l = Locale.getDefault();
                        if (Objects.equals(dl, l.getDisplayLanguage()))
                            ref_lang = null;
                    } else {
                        LocaleList ll = getResources().getConfiguration().getLocales();
                        for (int i = 0; i < ll.size(); i++) {
                            Locale l = ll.get(i);
                            if (Objects.equals(dl, l.getDisplayLanguage())) {
                                ref_lang = null;
                                break;
                            }
                        }
                    }
                }

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                boolean ref_hint = prefs.getBoolean("compose_reference", true);
                boolean write_below = prefs.getBoolean("write_below", false);

                tvReference.setText(text[1]);
                tvReference.setVisibility(text[1] == null ? View.GONE : View.VISIBLE);
                grpReferenceHint.setVisibility(text[1] == null || !ref_hint ? View.GONE : View.VISIBLE);
                ibWriteAboveBelow.setImageLevel(write_below ? 1 : 0);
                ibWriteAboveBelow.setVisibility(text[1] == null ||
                        draft.wasforwardedfrom != null || BuildConfig.PLAY_STORE_RELEASE
                        ? View.GONE : View.VISIBLE);
                tvLanguage.setText(ref_lang == null ? null : ref_lang.getDisplayLanguage());
                tvLanguage.setVisibility(ref_lang == null ? View.GONE : View.VISIBLE);
                ibReferenceEdit.setVisibility(text[1] == null ? View.GONE : View.VISIBLE);
                ibReferenceImages.setVisibility(ref_has_images && !show_images ? View.VISIBLE : View.GONE);

                setBodyPadding();

                if (refedit && write_below)
                    etBody.setSelection(etBody.length());

                if (state == State.LOADED)
                    return;
                state = State.LOADED;

                int selStart = (selection == 0 ? -1 : selection);

                if (selStart < 0) {
                    int pos = getAutoPos(0, etBody.length());
                    if (pos < 0)
                        pos = 0;
                    etBody.setSelection(pos);
                }

                setFocus(selStart < 0 ? null : R.id.etBody, selStart, selStart, postShow == null);
                if (postShow != null)
                    getMainHandler().post(postShow);
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }.setExecutor(executor).execute(this, args, "compose:show");
    }

    private void setFocus(Integer v, int start, int end, boolean restore) {
        final View target;
        if (v != null)
            target = view.findViewById(v);
        else if (TextUtils.isEmpty(etTo.getText().toString().trim()))
            target = etTo;
        else if (TextUtils.isEmpty(etSubject.getText().toString()))
            target = etSubject;
        else
            target = etBody;

        if (target == null)
            return;

        int s = (start < end ? start : end);
        int e = (start < end ? end : start);

        getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                        return;

                    if (target instanceof EditText) {
                        EditText et = (EditText) target;
                        int len = et.length();
                        if (s >= 0 && s <= len && e <= len)
                            if (e < 0)
                                et.setSelection(s);
                            else
                                et.setSelection(s, e);
                    }

                    target.requestFocus();

                    Context context = target.getContext();
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    boolean keyboard = prefs.getBoolean("keyboard", true);
                    if (keyboard && restore)
                        Helper.showKeyboard(target);

                } catch (Throwable ex) {
                    Log.e(ex);
                }
            }
        });
    }

    private void setBodyPadding() {
        // Keep room for the style toolbar
        boolean pad =
                (grpSignature.getVisibility() == View.GONE &&
                        tvReference.getVisibility() == View.GONE);
        etBody.setPadding(0, 0, 0, pad ? Helper.dp2pixels(getContext(), 36) : 0);
    }

    private int getAutoPos(int start, int end) {
        if (start > end || end == 0)
            return -1;

        CharSequence text = etBody.getText();
        if (text == null)
            return -1;

        int lc = 0;
        int nl = 0;
        int pos = 0;
        String[] lines = text.subSequence(start, end).toString().split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (TextUtils.isEmpty(lines[i]))
                nl++;
            else {
                lc++;
                nl = 0;
            }
            if (lc > 1)
                return -1;
            if (nl > 2)
                return start + pos - 1;
            pos += lines[i].length() + 1;
        }
        return -1;
    }

    private void startSearch() {
        etSearch.setText(null);
        etSearch.setVisibility(View.VISIBLE);
        etSearch.requestFocus();
        Helper.showKeyboard(etSearch);
    }

    private void endSearch() {
        if (etSearch == null)
            return;

        Helper.hideKeyboard(etSearch);
        etSearch.setVisibility(View.GONE);
        clearSearch();
    }

    private void performSearch(boolean next) {
        clearSearch();

        searchIndex = (next ? searchIndex + 1 : 1);
        String query = etSearch.getText().toString().toLowerCase();
        String text = etBody.getText().toString().toLowerCase();

        int pos = -1;
        for (int i = 0; i < searchIndex; i++)
            pos = (pos < 0 ? text.indexOf(query) : text.indexOf(query, pos + 1));

        // Wrap around
        if (pos < 0 && searchIndex > 1) {
            searchIndex = 1;
            pos = text.indexOf(query);
        }

        // Scroll to found text
        if (pos >= 0) {
            Context context = etBody.getContext();
            int color = Helper.resolveColor(context, R.attr.colorHighlight);
            SpannableString ss = new SpannableString(etBody.getText());
            ss.setSpan(new BackgroundColorSpan(color),
                    pos, pos + query.length(), Spannable.SPAN_COMPOSING);
            ss.setSpan(new RelativeSizeSpan(HtmlHelper.FONT_LARGE),
                    pos, pos + query.length(), Spannable.SPAN_COMPOSING);
            etBody.setText(ss);

            Layout layout = etBody.getLayout();
            if (layout != null) {
                int line = layout.getLineForOffset(pos);
                int y = layout.getLineTop(line);
                int dy = context.getResources().getDimensionPixelSize(R.dimen.search_in_text_margin);

                Rect rect = new Rect();
                etBody.getDrawingRect(rect);
                ScrollView scroll = view.findViewById(R.id.scroll);
                scroll.offsetDescendantRectToMyCoords(etBody, rect);
                scroll.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            scroll.scrollTo(0, rect.top + y - dy);
                        } catch (Throwable ex) {
                            Log.e(ex);
                        }
                    }
                });
            }
        }

        boolean hasNext = (pos >= 0 &&
                (text.indexOf(query) != pos ||
                        text.indexOf(query, pos + 1) >= 0));
        etSearch.setActionEnabled(hasNext);
    }

    private void clearSearch() {
        etBody.clearComposingText();
    }

    private AdapterView.OnItemSelectedListener identitySelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            EntityIdentity identity = (EntityIdentity) parent.getAdapter().getItem(position);

            int at = (identity == null ? -1 : identity.email.indexOf('@'));
            etExtra.setHint(at < 0 ? null : identity.email.substring(0, at));
            tvDomain.setText(at < 0 ? null : identity.email.substring(at));
            grpExtra.setVisibility(identity != null && identity.sender_extra ? View.VISIBLE : View.GONE);

            Spanned signature = null;
            if (identity != null && !TextUtils.isEmpty(identity.signature))
                signature = HtmlHelper.fromHtml(identity.signature, new HtmlHelper.ImageGetterEx() {
                    @Override
                    public Drawable getDrawable(Element element) {
                        String source = element.attr("src");
                        if (source.startsWith("cid:"))
                            element.attr("src", "cid:");
                        return ImageHelper.decodeImage(getContext(),
                                working, element, true, 0, 1.0f, tvSignature);
                    }
                }, null, getContext());
            tvSignature.setText(signature);
            grpSignature.setVisibility(signature == null ? View.GONE : View.VISIBLE);

            setBodyPadding();

            if (!Objects.equals(spIdentity.getTag(), position)) {
                spIdentity.setTag(position);
                updateEncryption(identity);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            etExtra.setHint("");
            tvDomain.setText(null);

            tvSignature.setText(null);
            grpSignature.setVisibility(View.GONE);

            setBodyPadding();

            updateEncryption(null);
        }

        private void updateEncryption(EntityIdentity identity) {
            if (identity == null)
                return;

            Bundle args = new Bundle();
            args.putLong("id", working);
            args.putLong("identity", identity.id);

            new SimpleTask<Integer>() {
                @Override
                protected Integer onExecute(Context context, Bundle args) {
                    long id = args.getLong("id");
                    long iid = args.getLong("identity");

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    boolean sign_default = prefs.getBoolean("sign_default", false);
                    boolean encrypt_default = prefs.getBoolean("encrypt_default", false);

                    DB db = DB.getInstance(context);

                    EntityMessage draft = db.message().getMessage(id);
                    if (draft == null)
                        return null;

                    if (draft.dsn != null && !EntityMessage.DSN_NONE.equals(draft.dsn))
                        return null;

                    EntityIdentity identity = db.identity().getIdentity(iid);
                    if (identity == null)
                        return draft.ui_encrypt;

                    if (encrypt_default || identity.encrypt_default)
                        draft.ui_encrypt = EntityMessage.PGP_SIGNENCRYPT;
                    else if (sign_default || identity.sign_default)
                        draft.ui_encrypt = EntityMessage.PGP_SIGNONLY;
                    else
                        draft.ui_encrypt = null;

                    if (identity.encrypt == 0) {
                        if (EntityMessage.SMIME_SIGNONLY.equals(draft.ui_encrypt))
                            draft.ui_encrypt = EntityMessage.PGP_SIGNONLY;
                        else if (EntityMessage.SMIME_SIGNENCRYPT.equals(draft.ui_encrypt))
                            draft.ui_encrypt = EntityMessage.PGP_SIGNENCRYPT;
                    } else {
                        if (EntityMessage.PGP_SIGNONLY.equals(draft.ui_encrypt))
                            draft.ui_encrypt = EntityMessage.SMIME_SIGNONLY;
                        else if (EntityMessage.PGP_SIGNENCRYPT.equals(draft.ui_encrypt))
                            draft.ui_encrypt = EntityMessage.SMIME_SIGNENCRYPT;
                    }

                    db.message().setMessageUiEncrypt(draft.id, draft.ui_encrypt);

                    return draft.ui_encrypt;
                }

                @Override
                protected void onExecuted(Bundle args, Integer encrypt) {
                    FragmentCompose.this.encrypt = encrypt;
                }

                @Override
                protected void onException(Bundle args, Throwable ex) {
                    Log.unexpectedError(getParentFragmentManager(), ex);
                }
            }.setExecutor(executor).execute(FragmentCompose.this, args, "compose:identity");
        }
    };

    private ActivityBase.IKeyPressedListener onKeyPressedListener = new ActivityBase.IKeyPressedListener() {
        @Override
        public boolean onKeyPressed(KeyEvent event) {
            if (event.isCtrlPressed() && event.getAction() == KeyEvent.ACTION_UP) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_S:
                    case KeyEvent.KEYCODE_ENTER:
                        onAction(R.id.action_check, "key");
                        return true;
                    case KeyEvent.KEYCODE_B:
                        if (etBody.hasSelection())
                            return StyleHelper.apply(R.id.menu_bold, getViewLifecycleOwner(), null, etBody);
                        else
                            return false;
                    case KeyEvent.KEYCODE_I:
                        if (etBody.hasSelection())
                            return StyleHelper.apply(R.id.menu_italic, getViewLifecycleOwner(), null, etBody);
                        else
                            return false;
                    case KeyEvent.KEYCODE_U:
                        if (etBody.hasSelection())
                            return StyleHelper.apply(R.id.menu_underline, getViewLifecycleOwner(), null, etBody);
                        else
                            return false;
                }
            }

            return false;
        }
    };

    private OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (Helper.isKeyboardVisible(view))
                Helper.hideKeyboard(view);
            else
                onExit();
        }
    };

    public static class FragmentDialogContactGroup extends FragmentDialogBase {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            final Bundle args = getArguments();
            final long working = args.getLong("working");
            int focussed = args.getInt("focussed");

            final Context context = getContext();
            View dview = LayoutInflater.from(context).inflate(R.layout.dialog_contact_group, null);
            final ImageButton ibInfo = dview.findViewById(R.id.ibInfo);
            final Spinner spGroup = dview.findViewById(R.id.spGroup);
            final Spinner spTarget = dview.findViewById(R.id.spTarget);
            final Spinner spType = dview.findViewById(R.id.spType);

            ibInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Helper.view(v.getContext(), Uri.parse(Helper.URI_SUPPORT_CONTACT_GROUP), true);
                }
            });

            new SimpleTask<Cursor>() {
                @Override
                protected Cursor onExecute(Context context, Bundle args) {
                    final String[] projection = new String[]{
                            ContactsContract.Groups._ID,
                            ContactsContract.Groups.TITLE,
                            ContactsContract.Groups.SUMMARY_COUNT,
                            ContactsContract.Groups.ACCOUNT_NAME,
                            ContactsContract.Groups.ACCOUNT_TYPE,
                    };

                    Cursor contacts = new MatrixCursor(projection);
                    if (Helper.hasPermission(context, Manifest.permission.READ_CONTACTS))
                        try {
                            ContentResolver resolver = context.getContentResolver();
                            contacts = resolver.query(
                                    ContactsContract.Groups.CONTENT_SUMMARY_URI,
                                    projection,
                                    // ContactsContract.Groups.GROUP_VISIBLE + " = 1" + " AND " +
                                    ContactsContract.Groups.DELETED + " = 0" +
                                            " AND " + ContactsContract.Groups.SUMMARY_COUNT + " > 0",
                                    null,
                                    ContactsContract.Groups.TITLE
                            );
                        } catch (SecurityException ex) {
                            Log.w(ex);
                        }

                    DB db = DB.getInstance(context);
                    Cursor local = db.contact().getGroups(
                            null,
                            context.getString(R.string.app_name),
                            BuildConfig.APPLICATION_ID);

                    return new MergeCursor(new Cursor[]{contacts, local});
                }

                @Override
                protected void onExecuted(Bundle args, Cursor cursor) {
                    SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                            context,
                            R.layout.spinner_contact_group,
                            cursor,
                            new String[]{ContactsContract.Groups.TITLE, ContactsContract.Groups.ACCOUNT_NAME},
                            new int[]{R.id.tvGroup, R.id.tvAccount},
                            0);

                    final NumberFormat NF = NumberFormat.getInstance();

                    adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
                        @Override
                        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                            if (view.getId() == R.id.tvGroup) {
                                String title = cursor.getString(1);
                                if (TextUtils.isEmpty(title))
                                    title = "-";
                                int count = cursor.getInt(2);
                                ((TextView) view).setText(context.getString(R.string.title_name_count, title, NF.format(count)));
                                return true;
                            } else if (view.getId() == R.id.tvAccount) {
                                String account = cursor.getString(3);
                                String type = cursor.getString(4);
                                ((TextView) view).setText(account + (BuildConfig.DEBUG ? "/" + type : ""));
                                return true;
                            } else
                                return false;
                        }
                    });

                    spGroup.setAdapter(adapter);
                }

                @Override
                protected void onException(Bundle args, Throwable ex) {
                    Log.unexpectedError(getParentFragmentManager(), ex);
                }
            }.execute(this, new Bundle(), "compose:groups");

            spTarget.setSelection(focussed);

            return new AlertDialog.Builder(context)
                    .setView(dview)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            int target = spTarget.getSelectedItemPosition();
                            Cursor cursor = (Cursor) spGroup.getSelectedItem();
                            if (target != INVALID_POSITION && cursor != null) {
                                long group = cursor.getLong(0);
                                String name = cursor.getString(1);

                                Bundle args = getArguments();
                                args.putLong("id", working);
                                args.putInt("target", target);
                                args.putLong("group", group);
                                args.putString("name", name);
                                args.putInt("type", spType.getSelectedItemPosition());

                                sendResult(RESULT_OK);
                            } else
                                sendResult(RESULT_CANCELED);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        }
    }

    public static class FragmentDialogAddImage extends FragmentDialogBase {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            int title = getArguments().getInt("title");

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            boolean add_inline = prefs.getBoolean("add_inline", true);
            boolean resize_images = prefs.getBoolean("resize_images", true);
            int resize = prefs.getInt("resize", FragmentCompose.REDUCED_IMAGE_SIZE);
            boolean privacy_images = prefs.getBoolean("privacy_images", false);
            boolean image_dialog = prefs.getBoolean("image_dialog", true);

            final ViewGroup dview = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_image, null);
            final ImageView ivType = dview.findViewById(R.id.ivType);
            final RadioGroup rgAction = dview.findViewById(R.id.rgAction);
            final CheckBox cbResize = dview.findViewById(R.id.cbResize);
            final ImageButton ibResize = dview.findViewById(R.id.ibResize);
            final Spinner spResize = dview.findViewById(R.id.spResize);
            final TextView tvResize = dview.findViewById(R.id.tvResize);
            final CheckBox cbPrivacy = dview.findViewById(R.id.cbPrivacy);
            final CheckBox cbNotAgain = dview.findViewById(R.id.cbNotAgain);
            final TextView tvNotAgain = dview.findViewById(R.id.tvNotAgain);

            ivType.setImageResource(title == R.string.title_attachment_photo
                    ? R.drawable.twotone_photo_camera_24 : R.drawable.twotone_image_24);
            rgAction.check(add_inline ? R.id.rbInline : R.id.rbAttach);
            cbResize.setChecked(resize_images);
            spResize.setEnabled(resize_images);
            cbPrivacy.setChecked(privacy_images);

            final int[] resizeValues = getResources().getIntArray(R.array.resizeValues);
            for (int pos = 0; pos < resizeValues.length; pos++)
                if (resizeValues[pos] == resize) {
                    spResize.setSelection(pos);
                    tvResize.setText(getString(R.string.title_add_resize_pixels, resizeValues[pos]));
                    break;
                }

            rgAction.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    prefs.edit().putBoolean("add_inline", checkedId == R.id.rbInline).apply();
                }
            });

            cbResize.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    prefs.edit().putBoolean("resize_images", isChecked).apply();
                    spResize.setEnabled(isChecked);
                }
            });

            ibResize.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Helper.viewFAQ(v.getContext(), 63);
                }
            });

            spResize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                    prefs.edit().putInt("resize", resizeValues[position]).apply();
                    tvResize.setText(getString(R.string.title_add_resize_pixels, resizeValues[position]));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    prefs.edit().remove("resize").apply();
                }
            });

            cbPrivacy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    prefs.edit().putBoolean("privacy_images", isChecked).apply();
                }
            });

            cbNotAgain.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    prefs.edit().putBoolean("image_dialog", !isChecked).apply();
                    tvNotAgain.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });

            cbNotAgain.setChecked(!image_dialog);
            tvNotAgain.setVisibility(cbNotAgain.isChecked() ? View.VISIBLE : View.GONE);

            return new AlertDialog.Builder(getContext())
                    .setView(dview)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(title,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    sendResult(RESULT_OK);
                                }
                            })
                    .create();
        }
    }

    public static class FragmentDialogSend extends FragmentDialogBase {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            Bundle args = getArguments();
            long id = args.getLong("id");
            final boolean sent_missing = args.getBoolean("sent_missing", false);
            final String address_error = args.getString("address_error");
            final String mx_error = args.getString("mx_error");
            final boolean remind_dsn = args.getBoolean("remind_dsn", false);
            final boolean remind_size = args.getBoolean("remind_size", false);
            final boolean remind_pgp = args.getBoolean("remind_pgp", false);
            final boolean remind_smime = args.getBoolean("remind_smime", false);
            final boolean remind_to = args.getBoolean("remind_to", false);
            final boolean remind_extra = args.getBoolean("remind_extra", false);
            final boolean remind_noreply = args.getBoolean("remind_noreply", false);
            final boolean remind_external = args.getBoolean("remind_external", false);
            final boolean remind_subject = args.getBoolean("remind_subject", false);
            final boolean remind_text = args.getBoolean("remind_text", false);
            final boolean remind_attachment = args.getBoolean("remind_attachment", false);
            final boolean styled = args.getBoolean("styled", false);
            final long size = args.getLong("size", -1);
            final long max_size = args.getLong("max_size", -1);

            final Context context = getContext();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final boolean send_reminders = prefs.getBoolean("send_reminders", true);
            final int send_delayed = prefs.getInt("send_delayed", 0);
            final boolean send_dialog = prefs.getBoolean("send_dialog", true);
            final boolean send_archive = prefs.getBoolean("send_archive", false);
            final MessageHelper.AddressFormat email_format = MessageHelper.getAddressFormat(getContext());

            final int[] encryptValues = getResources().getIntArray(R.array.encryptValues);
            final int[] sendDelayedValues = getResources().getIntArray(R.array.sendDelayedValues);
            final String[] sendDelayedNames = getResources().getStringArray(R.array.sendDelayedNames);

            final ViewGroup dview = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.dialog_send, null);
            final Button btnFixSent = dview.findViewById(R.id.btnFixSent);
            final TextView tvAddressError = dview.findViewById(R.id.tvAddressError);
            final TextView tvRemindDsn = dview.findViewById(R.id.tvRemindDsn);
            final TextView tvRemindSize = dview.findViewById(R.id.tvRemindSize);
            final TextView tvRemindPgp = dview.findViewById(R.id.tvRemindPgp);
            final TextView tvRemindSmime = dview.findViewById(R.id.tvRemindSmime);
            final TextView tvRemindTo = dview.findViewById(R.id.tvRemindTo);
            final TextView tvRemindExtra = dview.findViewById(R.id.tvRemindExtra);
            final TextView tvRemindNoReply = dview.findViewById(R.id.tvRemindNoReply);
            final TextView tvRemindExternal = dview.findViewById(R.id.tvRemindExternal);
            final TextView tvRemindSubject = dview.findViewById(R.id.tvRemindSubject);
            final TextView tvRemindText = dview.findViewById(R.id.tvRemindText);
            final TextView tvRemindAttachment = dview.findViewById(R.id.tvRemindAttachment);
            final SwitchCompat swSendReminders = dview.findViewById(R.id.swSendReminders);
            final TextView tvSendRemindersHint = dview.findViewById(R.id.tvSendRemindersHint);
            final TextView tvTo = dview.findViewById(R.id.tvTo);
            final TextView tvViaTitle = dview.findViewById(R.id.tvViaTitle);
            final TextView tvVia = dview.findViewById(R.id.tvVia);
            final CheckBox cbPlainOnly = dview.findViewById(R.id.cbPlainOnly);
            final TextView tvPlainHint = dview.findViewById(R.id.tvPlainHint);
            final CheckBox cbReceipt = dview.findViewById(R.id.cbReceipt);
            final TextView tvReceiptHint = dview.findViewById(R.id.tvReceiptHint);
            final TextView tvEncrypt = dview.findViewById(R.id.tvEncrypt);
            final Spinner spEncrypt = dview.findViewById(R.id.spEncrypt);
            final ImageButton ibEncryption = dview.findViewById(R.id.ibEncryption);
            final Spinner spPriority = dview.findViewById(R.id.spPriority);
            final Spinner spSensitivity = dview.findViewById(R.id.spSensitivity);
            final ImageButton ibSensitivity = dview.findViewById(R.id.ibSensitivity);
            final TextView tvSendAt = dview.findViewById(R.id.tvSendAt);
            final ImageButton ibSendAt = dview.findViewById(R.id.ibSendAt);
            final CheckBox cbArchive = dview.findViewById(R.id.cbArchive);
            final CheckBox cbNotAgain = dview.findViewById(R.id.cbNotAgain);
            final TextView tvNotAgain = dview.findViewById(R.id.tvNotAgain);
            final Group grpSentMissing = dview.findViewById(R.id.grpSentMissing);
            final Group grpDsn = dview.findViewById(R.id.grpDsn);

            btnFixSent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.getContext().startActivity(new Intent(v.getContext(), ActivitySetup.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            .putExtra("target", "accounts"));
                }
            });

            grpSentMissing.setVisibility(sent_missing ? View.VISIBLE : View.GONE);

            tvAddressError.setText(address_error == null ? mx_error : address_error);
            tvAddressError.setVisibility(address_error == null && mx_error == null ? View.GONE : View.VISIBLE);

            tvRemindDsn.setVisibility(remind_dsn ? View.VISIBLE : View.GONE);

            tvRemindSize.setText(getString(R.string.title_size_reminder,
                    Helper.humanReadableByteCount(size),
                    Helper.humanReadableByteCount(max_size)));
            tvRemindSize.setVisibility(remind_size ? View.VISIBLE : View.GONE);

            tvRemindPgp.setVisibility(remind_pgp ? View.VISIBLE : View.GONE);
            tvRemindSmime.setVisibility(remind_smime ? View.VISIBLE : View.GONE);

            tvRemindTo.setVisibility(remind_to ? View.VISIBLE : View.GONE);
            tvRemindExtra.setVisibility(send_reminders && remind_extra ? View.VISIBLE : View.GONE);
            tvRemindNoReply.setVisibility(remind_noreply ? View.VISIBLE : View.GONE);
            tvRemindExternal.setVisibility(remind_external ? View.VISIBLE : View.GONE);
            tvRemindSubject.setVisibility(send_reminders && remind_subject ? View.VISIBLE : View.GONE);
            tvRemindText.setVisibility(send_reminders && remind_text ? View.VISIBLE : View.GONE);
            tvRemindAttachment.setVisibility(send_reminders && remind_attachment ? View.VISIBLE : View.GONE);

            tvTo.setText(null);
            tvVia.setText(null);
            tvPlainHint.setVisibility(View.GONE);
            tvReceiptHint.setVisibility(View.GONE);
            spEncrypt.setTag(0);
            spEncrypt.setSelection(0);
            spPriority.setTag(1);
            spPriority.setSelection(1);
            spSensitivity.setTag(0);
            spSensitivity.setSelection(0);
            tvSendAt.setText(null);
            cbArchive.setEnabled(false);
            cbNotAgain.setChecked(!send_dialog);
            cbNotAgain.setVisibility(send_dialog ? View.VISIBLE : View.GONE);
            tvNotAgain.setVisibility(cbNotAgain.isChecked() ? View.VISIBLE : View.GONE);

            Helper.setViewsEnabled(dview, false);

            boolean reminder = (remind_extra || remind_subject || remind_text || remind_attachment);
            swSendReminders.setChecked(send_reminders);
            swSendReminders.setVisibility(send_reminders && reminder ? View.VISIBLE : View.GONE);
            tvSendRemindersHint.setVisibility(View.GONE);
            swSendReminders.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    prefs.edit().putBoolean("send_reminders", checked).apply();
                    tvRemindExtra.setVisibility(checked && remind_extra ? View.VISIBLE : View.GONE);
                    tvRemindSubject.setVisibility(checked && remind_subject ? View.VISIBLE : View.GONE);
                    tvRemindText.setVisibility(checked && remind_text ? View.VISIBLE : View.GONE);
                    tvRemindAttachment.setVisibility(checked && remind_attachment ? View.VISIBLE : View.GONE);
                    tvSendRemindersHint.setVisibility(checked ? View.GONE : View.VISIBLE);
                }
            });

            cbNotAgain.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    prefs.edit().putBoolean("send_dialog", !isChecked).apply();
                    tvNotAgain.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });

            cbPlainOnly.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    tvPlainHint.setVisibility(checked && styled ? View.VISIBLE : View.GONE);

                    Bundle args = new Bundle();
                    args.putLong("id", id);
                    args.putBoolean("plain_only", checked);

                    new SimpleTask<Void>() {
                        @Override
                        protected Void onExecute(Context context, Bundle args) {
                            long id = args.getLong("id");
                            boolean plain_only = args.getBoolean("plain_only");

                            DB db = DB.getInstance(context);
                            db.message().setMessagePlainOnly(id, plain_only ? 1 : 0);

                            return null;
                        }

                        @Override
                        protected void onException(Bundle args, Throwable ex) {
                            Log.unexpectedError(getParentFragmentManager(), ex);
                        }
                    }.setExecutor(executor).execute(FragmentDialogSend.this, args, "compose:plain_only");
                }
            });

            cbReceipt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    tvReceiptHint.setVisibility(checked ? View.VISIBLE : View.GONE);

                    Bundle args = new Bundle();
                    args.putLong("id", id);
                    args.putBoolean("receipt", checked);

                    new SimpleTask<Void>() {
                        @Override
                        protected Void onExecute(Context context, Bundle args) {
                            long id = args.getLong("id");
                            boolean receipt = args.getBoolean("receipt");

                            DB db = DB.getInstance(context);
                            db.message().setMessageReceiptRequest(id, receipt);

                            return null;
                        }

                        @Override
                        protected void onException(Bundle args, Throwable ex) {
                            Log.unexpectedError(getParentFragmentManager(), ex);
                        }
                    }.setExecutor(executor).execute(FragmentDialogSend.this, args, "compose:receipt");
                }
            });

            if (Helper.isOpenKeychainInstalled(context)) {
                tvEncrypt.setPaintFlags(tvEncrypt.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                tvEncrypt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String pkg = Helper.getOpenKeychainPackage(v.getContext());
                        PackageManager pm = v.getContext().getPackageManager();
                        v.getContext().startActivity(pm.getLaunchIntentForPackage(pkg));
                    }
                });
            }

            spEncrypt.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    int last = (int) spEncrypt.getTag();
                    if (last != position) {
                        spEncrypt.setTag(position);
                        setEncrypt(encryptValues[position]);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    spEncrypt.setTag(0);
                    setEncrypt(encryptValues[0]);
                }

                private void setEncrypt(int encrypt) {
                    Bundle args = new Bundle();
                    args.putLong("id", id);
                    args.putInt("encrypt", encrypt);

                    new SimpleTask<Void>() {
                        @Override
                        protected Void onExecute(Context context, Bundle args) {
                            long id = args.getLong("id");
                            int encrypt = args.getInt("encrypt");

                            DB db = DB.getInstance(context);
                            try {
                                db.beginTransaction();

                                EntityMessage message = db.message().getMessage(id);
                                if (message == null)
                                    return null;

                                db.message().setMessageUiEncrypt(message.id, encrypt);

                                if (encrypt != EntityMessage.ENCRYPT_NONE &&
                                        message.identity != null) {
                                    int iencrypt =
                                            (encrypt == EntityMessage.SMIME_SIGNONLY ||
                                                    encrypt == EntityMessage.SMIME_SIGNENCRYPT
                                                    ? 1 : 0);
                                    db.identity().setIdentityEncrypt(message.identity, iencrypt);
                                }

                                db.setTransactionSuccessful();
                            } finally {
                                db.endTransaction();
                            }

                            return null;
                        }

                        @Override
                        protected void onException(Bundle args, Throwable ex) {
                            Log.unexpectedError(getParentFragmentManager(), ex);
                        }
                    }.setExecutor(executor).execute(FragmentDialogSend.this, args, "compose:encrypt");
                }
            });

            ibEncryption.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Helper.viewFAQ(v.getContext(), 12);
                }
            });

            spPriority.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    int last = (int) spPriority.getTag();
                    if (last != position) {
                        spPriority.setTag(position);
                        setPriority(position);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    spPriority.setTag(1);
                    setPriority(1);
                }

                private void setPriority(int priority) {
                    Bundle args = new Bundle();
                    args.putLong("id", id);
                    args.putInt("priority", priority);

                    new SimpleTask<Void>() {
                        @Override
                        protected Void onExecute(Context context, Bundle args) {
                            long id = args.getLong("id");
                            int priority = args.getInt("priority");

                            DB db = DB.getInstance(context);
                            db.message().setMessagePriority(id, priority);

                            return null;
                        }

                        @Override
                        protected void onException(Bundle args, Throwable ex) {
                            Log.unexpectedError(getParentFragmentManager(), ex);
                        }
                    }.setExecutor(executor).execute(FragmentDialogSend.this, args, "compose:priority");
                }
            });

            spSensitivity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    int last = (int) spSensitivity.getTag();
                    if (last != position) {
                        spSensitivity.setTag(position);
                        setSensitivity(position);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    spSensitivity.setTag(0);
                    setSensitivity(0);
                }

                private void setSensitivity(int sensitivity) {
                    Bundle args = new Bundle();
                    args.putLong("id", id);
                    args.putInt("sensitivity", sensitivity);

                    new SimpleTask<Void>() {
                        @Override
                        protected Void onExecute(Context context, Bundle args) {
                            long id = args.getLong("id");
                            int sensitivity = args.getInt("sensitivity");

                            DB db = DB.getInstance(context);
                            db.message().setMessageSensitivity(id, sensitivity < 1 ? null : sensitivity);

                            return null;
                        }

                        @Override
                        protected void onException(Bundle args, Throwable ex) {
                            Log.unexpectedError(getParentFragmentManager(), ex);
                        }
                    }.setExecutor(executor).execute(FragmentDialogSend.this, args, "compose:sensitivity");
                }
            });

            ibSensitivity.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Helper.viewFAQ(v.getContext(), 177);
                }
            });

            View.OnClickListener sendAt = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Bundle args = new Bundle();
                    args.putString("title", getString(R.string.title_send_at));
                    args.putLong("id", id);

                    FragmentDialogDuration fragment = new FragmentDialogDuration();
                    fragment.setArguments(args);
                    fragment.setTargetFragment(FragmentDialogSend.this, 1);
                    fragment.show(getParentFragmentManager(), "send:snooze");
                }
            };

            tvSendAt.setOnClickListener(sendAt);
            ibSendAt.setOnClickListener(sendAt);

            cbArchive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    prefs.edit().putBoolean("send_archive", isChecked).apply();
                }
            });

            DB db = DB.getInstance(context);
            db.message().liveMessage(id).observe(getViewLifecycleOwner(), new Observer<TupleMessageEx>() {
                @Override
                public void onChanged(TupleMessageEx draft) {
                    if (draft == null) {
                        dismiss();
                        return;
                    }

                    boolean dsn = (draft.dsn != null && !EntityMessage.DSN_NONE.equals(draft.dsn));
                    int to = (draft.to == null ? 0 : draft.to.length);
                    int extra = (draft.cc == null ? 0 : draft.cc.length) + (draft.bcc == null ? 0 : draft.bcc.length);

                    List<Address> t = new ArrayList<>();
                    if (draft.to != null)
                        if (to <= MAX_SHOW_RECIPIENTS)
                            t.addAll(Arrays.asList(draft.to));
                        else {
                            t.addAll((Arrays.asList(Arrays.copyOf(draft.to, MAX_SHOW_RECIPIENTS))));
                            extra += draft.to.length - MAX_SHOW_RECIPIENTS;
                        }
                    Address[] tos = t.toArray(new Address[0]);

                    if (extra == 0)
                        tvTo.setText(MessageHelper.formatAddresses(tos, email_format, false));
                    else
                        tvTo.setText(getString(R.string.title_name_plus,
                                MessageHelper.formatAddresses(tos, email_format, false), extra));
                    tvTo.setTextColor(Helper.resolveColor(context,
                            to + extra > RECIPIENTS_WARNING ? R.attr.colorWarning : android.R.attr.textColorPrimary));
                    if (draft.identityColor != null && draft.identityColor != Color.TRANSPARENT)
                        tvViaTitle.setTextColor(draft.identityColor);
                    tvVia.setText(draft.identityEmail);

                    cbPlainOnly.setChecked(draft.isPlainOnly() && !dsn);
                    cbReceipt.setChecked(draft.receipt_request != null && draft.receipt_request && !dsn);

                    int encrypt = (draft.ui_encrypt == null || dsn ? EntityMessage.ENCRYPT_NONE : draft.ui_encrypt);
                    for (int i = 0; i < encryptValues.length; i++)
                        if (encryptValues[i] == encrypt) {
                            spEncrypt.setTag(i);
                            spEncrypt.setSelection(i);
                            break;
                        }

                    int priority = (draft.priority == null ? 1 : draft.priority);
                    spPriority.setTag(priority);
                    spPriority.setSelection(priority);

                    int sensitivity = (draft.sensitivity == null ? 0 : draft.sensitivity);
                    spSensitivity.setTag(sensitivity);
                    spSensitivity.setSelection(sensitivity);

                    if (draft.ui_snoozed == null) {
                        if (send_delayed == 0)
                            tvSendAt.setText(getString(R.string.title_now));
                        else
                            for (int pos = 0; pos < sendDelayedValues.length; pos++)
                                if (sendDelayedValues[pos] == send_delayed) {
                                    tvSendAt.setText(getString(R.string.title_after, sendDelayedNames[pos]));
                                    break;
                                }
                    } else {
                        DateFormat DTF = Helper.getDateTimeInstance(context, SimpleDateFormat.MEDIUM, SimpleDateFormat.SHORT);
                        DateFormat D = new SimpleDateFormat("E");
                        tvSendAt.setText(D.format(draft.ui_snoozed) + " " + DTF.format(draft.ui_snoozed));
                    }

                    grpDsn.setVisibility(dsn ? View.GONE : View.VISIBLE);

                    Helper.setViewsEnabled(dview, true);
                }
            });

            Bundle aargs = new Bundle();
            aargs.putLong("id", id);

            new SimpleTask<Boolean>() {
                @Override
                protected @NonNull
                Boolean onExecute(Context context, Bundle args) {
                    long id = args.getLong("id");

                    DB db = DB.getInstance(context);
                    EntityMessage draft = db.message().getMessage(id);
                    if (draft == null) {
                        args.putString("reason", "Draft gone");
                        return false;
                    }

                    if (TextUtils.isEmpty(draft.inreplyto)) {
                        args.putString("reason", "No in-reply-to");
                        return false;
                    }

                    EntityFolder archive = db.folder().getFolderByType(draft.account, EntityFolder.ARCHIVE);
                    if (archive == null) {
                        args.putString("reason", "No archive");
                        return false;
                    }

                    List<EntityMessage> messages = db.message().getMessagesByMsgId(draft.account, draft.inreplyto);
                    if (messages == null || messages.size() == 0) {
                        args.putString("reason", "In-reply-to gone");
                        return false;
                    }

                    for (EntityMessage message : messages) {
                        EntityFolder folder = db.folder().getFolder(message.folder);
                        if (folder == null)
                            continue;
                        if (EntityFolder.INBOX.equals(folder.type) || EntityFolder.USER.equals(folder.type))
                            return true;
                    }

                    args.putString("reason", "Not in inbox or unread");
                    return false;
                }

                @Override
                protected void onExecuted(Bundle args, Boolean data) {
                    if (!data) {
                        String reason = args.getString("reason");
                        if (BuildConfig.DEBUG)
                            cbArchive.setText(reason);
                        else
                            Log.i("Auto archive reason=" + reason);
                    }
                    if (send_archive && data)
                        cbArchive.setChecked(true);
                    cbArchive.setEnabled(data);
                }

                @Override
                protected void onException(Bundle args, Throwable ex) {
                    // Ignored
                }
            }.setExecutor(executor).execute(FragmentDialogSend.this, aargs, "send:archive");

            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setView(dview)
                    .setNegativeButton(android.R.string.cancel, null);

            if (address_error == null && !remind_to && !remind_size) {
                if (send_delayed != 0)
                    builder.setNeutralButton(R.string.title_send_now, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getArguments().putBoolean("archive", cbArchive.isChecked());
                            sendResult(Activity.RESULT_FIRST_USER);
                        }
                    });
                builder.setPositiveButton(R.string.title_send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getArguments().putBoolean("archive", cbArchive.isChecked());
                        sendResult(Activity.RESULT_OK);
                    }
                });
            }

            return builder.create();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
            super.onActivityResult(requestCode, resultCode, intent);

            if (resultCode == RESULT_OK && intent != null) {
                Bundle data = intent.getBundleExtra("args");
                long id = data.getLong("id");
                long duration = data.getLong("duration");
                long time = data.getLong("time");

                Bundle args = new Bundle();
                args.putLong("id", id);
                args.putLong("wakeup", duration == 0 ? -1 : time);

                new SimpleTask<Void>() {
                    @Override
                    protected Void onExecute(Context context, Bundle args) {
                        long id = args.getLong("id");
                        long wakeup = args.getLong("wakeup");

                        DB db = DB.getInstance(context);
                        db.message().setMessageSnoozed(id, wakeup < 0 ? null : wakeup);

                        return null;
                    }

                    @Override
                    protected void onException(Bundle args, Throwable ex) {
                        Log.unexpectedError(getParentFragmentManager(), ex);
                    }
                }.setExecutor(executor).execute(this, args, "compose:snooze");
            }
        }
    }

    @NonNull
    private static UriInfo getInfo(Uri uri, Context context) {
        UriInfo result = new UriInfo();
        try {
            DocumentFile dfile = DocumentFile.fromSingleUri(context, uri);
            if (dfile != null) {
                result.name = dfile.getName();
                result.type = dfile.getType();
                result.size = dfile.length();
            }
        } catch (SecurityException ex) {
            Log.e(ex);
        }

        // Check name
        if (TextUtils.isEmpty(result.name))
            result.name = uri.getLastPathSegment();

        // Check type
        if (!TextUtils.isEmpty(result.type))
            try {
                new ContentType(result.type);
            } catch (ParseException ex) {
                Log.w(ex);
                result.type = null;
            }

        if (TextUtils.isEmpty(result.type) ||
                "*/*".equals(result.type) ||
                "application/*".equals(result.type) ||
                "application/octet-stream".equals(result.type))
            result.type = Helper.guessMimeType(result.name);

        if (result.size != null && result.size <= 0)
            result.size = null;

        return result;
    }

    private static class UriInfo {
        String name;
        String type;
        Long size;

        boolean isImage() {
            return ImageHelper.isImage(type);
        }
    }

    private static class DraftData {
        private EntityMessage draft;
        private List<TupleIdentityEx> identities;
    }
}
