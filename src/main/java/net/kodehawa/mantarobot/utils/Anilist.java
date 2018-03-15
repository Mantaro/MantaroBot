package net.kodehawa.mantarobot.utils;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import net.kodehawa.mantarobot.graphql.CharacterSearchQuery;
import net.kodehawa.mantarobot.graphql.MediaSearchQuery;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("all" /* i don't give a fuck that the call to Page(), characters() or media() can NPE */)
public class Anilist {
    private static final ApolloClient CLIENT = ApolloClient.builder()
            .serverUrl("https://graphql.anilist.co")
            .okHttpClient(Utils.httpClient)
            .build();

    public static List<CharacterSearchQuery.Character> searchCharacters(String query) {
        CompletableFuture<List<CharacterSearchQuery.Character>> future = new CompletableFuture<>();
        CLIENT.query(CharacterSearchQuery.builder()
                .query(query)
                .build()
        ).enqueue(new ApolloCall.Callback<CharacterSearchQuery.Data>() {
            @Override
            public void onResponse(@Nonnull Response<CharacterSearchQuery.Data> response) {
                future.complete(response.data().Page().characters());
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                future.completeExceptionally(e);
            }
        });
        try {
            return future.get();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<MediaSearchQuery.Medium> searchMedia(String query) {
        CompletableFuture<List<MediaSearchQuery.Medium>> future = new CompletableFuture<>();
        CLIENT.query(MediaSearchQuery.builder()
                .query(query)
                .build()
        ).enqueue(new ApolloCall.Callback<MediaSearchQuery.Data>() {
            @Override
            public void onResponse(@Nonnull Response<MediaSearchQuery.Data> response) {
                future.complete(response.data().Page().media());
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                future.completeExceptionally(e);
            }
        });
        try {
            return future.get();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
}
