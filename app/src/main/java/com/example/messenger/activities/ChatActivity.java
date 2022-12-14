package com.example.messenger.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import com.example.messenger.adapters.ChatAdapter;
import com.example.messenger.databinding.ActivityChatBinding;
import com.example.messenger.models.ChatMessage;
import com.example.messenger.models.User;
import com.example.messenger.utilities.Constants;
import com.example.messenger.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private PreferenceManager preferenceManager;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private FirebaseFirestore database;
    private String conversationId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loadReceiverDetails();
        setListeners();
        init();
        listenMessages();
    }

    public void init(){
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages,getBitmapFromEncodedString(receiverUser.image),preferenceManager.getString(Constants.KEY_USER_ID));
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void sendMessage(){
        HashMap<String,Object> sendingMessage = new HashMap<>();
        sendingMessage.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
        sendingMessage.put(Constants.KEY_RECEIVER_ID,receiverUser.id);
        String message = binding.inputMessage.getText().toString();
        sendingMessage.put(Constants.KEY_MESSAGE,message);
        sendingMessage.put(Constants.KEY_TIMESTAMP,new Date());
        if(conversationId != null){
            updateConversation(message);
        }else {
            HashMap<String, Object> conversation = new HashMap<>();
            conversation.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
            conversation.put(Constants.KEY_SENDER_NAME,preferenceManager.getString(Constants.KEY_NAME));
            conversation.put(Constants.KEY_SENDER_IMAGE,preferenceManager.getString(Constants.KEY_IMAGE));
            conversation.put(Constants.KEY_RECEIVER_ID,receiverUser.id);
            conversation.put(Constants.KEY_RECEIVER_NAME,receiverUser.name);
            conversation.put(Constants.KEY_RECEIVER_IMAGE,receiverUser.image);
            conversation.put(Constants.KEY_LAST_MESSAGE,message);
            conversation.put(Constants.KEY_TIMESTAMP, new Date());

            addConversation(conversation);
        }
        database.collection(Constants.KEY_COLLECTION_MESSAGES).add(sendingMessage);
        binding.inputMessage.setText(null);
    }

    private void listenMessages(){
        database.collection(Constants.KEY_COLLECTION_MESSAGES).whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID)).whereEqualTo(Constants.KEY_RECEIVER_ID,receiverUser.id).addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_MESSAGES).whereEqualTo(Constants.KEY_SENDER_ID,receiverUser.id).whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID)).addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value,error) -> {
        if(error != null){
            return;
        }
        if(value != null){
            int count = chatMessages.size();
            for(DocumentChange documentChange : value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages, Comparator.comparing(obj -> obj.dateObject));
            if(count == 0){
                chatAdapter.notifyDataSetChanged();
            }else {
                chatAdapter.notifyItemRangeChanged(chatMessages.size(),chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if(conversationId == null){
            checkForConversation();
        }

    };

    private Bitmap getBitmapFromEncodedString(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }

    private void loadReceiverDetails(){
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiverUser.name);
    }

    private void setListeners(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> {
            if(!binding.inputMessage.getText().toString().isEmpty()){
                sendMessage();
            }
        });
    }

    private String getReadableDateTime(Date date){
        return new SimpleDateFormat("MMMM dd yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversation(HashMap<String, Object> conversation){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).add(conversation).addOnSuccessListener(documentReference -> {
            conversationId = documentReference.getId();
        });
    }

    private void updateConversation(String message){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversationId).update(Constants.KEY_LAST_MESSAGE,message,Constants.KEY_TIMESTAMP,new Date());
    }

    private void checkForConversation(){
        if(chatMessages.size() != 0){
            checkForConversationRemotely(preferenceManager.getString(Constants.KEY_USER_ID), receiverUser.id);
        }
    }

    private void checkForConversationRemotely(String senderId,String receiverId){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).whereEqualTo(Constants.KEY_RECEIVER_ID,receiverId).whereEqualTo(Constants.KEY_SENDER_ID,senderId).get().addOnCompleteListener(conversationCompleteListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).whereEqualTo(Constants.KEY_RECEIVER_ID,senderId).whereEqualTo(Constants.KEY_SENDER_ID,receiverId).get().addOnCompleteListener(conversationCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversationCompleteListener = task -> {
        if(task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()){
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversationId = documentSnapshot.getId();
        }
    };
}