/*
 * Copyright (C) 2020 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit2.adapter.rxjava3;

import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST;

import io.reactivex.rxjava3.core.Completable;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Retrofit;
import retrofit2.http.GET;

public final class CompletableTest {
  @Rule public final MockWebServer server = new MockWebServer();

  @Rule
  public final RecordingCompletableObserver.Rule observerRule =
      new RecordingCompletableObserver.Rule();

  interface Service {
    @GET("/")
    Completable completable();
  }

  private Service service;

  @Before
  public void setUp() {
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addCallAdapterFactory(RxJava3CallAdapterFactory.createSynchronous())
            .build();
    service = retrofit.create(Service.class);
  }

  @Test
  public void completableSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    RecordingCompletableObserver observer = observerRule.create();
    service.completable().subscribe(observer);
    observer.assertComplete();
  }

  @Test
  public void completableSuccess404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    RecordingCompletableObserver observer = observerRule.create();
    service.completable().subscribe(observer);
    // Required for backwards compatibility.
    observer.assertError(HttpException.class, "HTTP 404 Client Error");
  }

  @Test
  public void completableFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    RecordingCompletableObserver observer = observerRule.create();
    service.completable().subscribe(observer);
    observer.assertError(IOException.class);
  }

  @Test
  public void subscribeTwice() {
    server.enqueue(new MockResponse().setBody("Hi"));
    server.enqueue(new MockResponse().setBody("Hey"));

    Completable observable = service.completable();

    RecordingCompletableObserver observer1 = observerRule.create();
    observable.subscribe(observer1);
    observer1.assertComplete();

    RecordingCompletableObserver observer2 = observerRule.create();
    observable.subscribe(observer2);
    observer2.assertComplete();
  }
}
