/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.bigtable.grpc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.api.client.util.NanoClock;
import com.google.bigtable.v1.BigtableServiceGrpc;
import com.google.bigtable.v1.CheckAndMutateRowRequest;
import com.google.bigtable.v1.CheckAndMutateRowResponse;
import com.google.bigtable.v1.MutateRowRequest;
import com.google.bigtable.v1.MutateRowsRequest;
import com.google.bigtable.v1.MutateRowsRequest.Entry;
import com.google.bigtable.v1.MutateRowsResponse;
import com.google.bigtable.v1.Mutation;
import com.google.bigtable.v1.Mutation.SetCell;
import com.google.bigtable.v1.ReadRowsRequest;
import com.google.bigtable.v1.RowRange;
import com.google.cloud.bigtable.config.BigtableOptions;
import com.google.cloud.bigtable.config.RetryOptions;
import com.google.cloud.bigtable.config.RetryOptionsUtil;
import com.google.cloud.bigtable.grpc.io.ChannelPool;
import com.google.cloud.bigtable.grpc.io.ClientCallService;
import com.google.common.base.Predicate;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.MethodDescriptor;

@RunWith(JUnit4.class)
@SuppressWarnings("unchecked")
public class BigtableDataGrpcClientTests {

  private static ExecutorService executor;

  @Mock
  ChannelPool channelPool;

  @SuppressWarnings("rawtypes")
  @Mock
  ClientCall clientCall;

  @Mock
  ClientCallService clientCallService;

  @SuppressWarnings("rawtypes")
  @Mock
  ListenableFuture mockFuture;

  @Mock
  NanoClock nanoClock;

  BigtableDataGrpcClient underTest;

  @After
  public void turndownExecutor() {
    executor.shutdownNow();
  }

  @Before
  public void setup() {
    executor = Executors.newSingleThreadExecutor();
    MockitoAnnotations.initMocks(this);
    RetryOptions retryOptions = RetryOptionsUtil.createTestRetryOptions(nanoClock);
    BigtableOptions options = new BigtableOptions.Builder().setRetryOptions(retryOptions).build();
    when(channelPool.newCall(any(MethodDescriptor.class), any(CallOptions.class))).thenReturn(
      clientCall);
    when(clientCallService.listenableAsyncCall(any(ClientCall.class), any())).thenReturn(mockFuture);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        invocation.getArgumentAt(0, Runnable.class).run();
        return null;
      }
    }).when(mockFuture).addListener(any(Runnable.class), any(Executor.class));

    underTest =
        new BigtableDataGrpcClient(
            channelPool,
            BigtableSessionSharedThreadPools.getInstance().getBatchThreadPool(),
            options,
            clientCallService);
  }

  @Test
  public void testRetyableMutateRow() throws Exception {
    final MutateRowRequest request = MutateRowRequest.getDefaultInstance();
    when(mockFuture.get()).thenReturn(Empty.getDefaultInstance());
    underTest.mutateRow(request);
    verify(clientCallService, times(1)).listenableAsyncCall(any(ClientCall.class), same(request));
  }

  @Test
  public void testRetyableMutateRowAsync() throws InterruptedException, ExecutionException {
    MutateRowRequest request = MutateRowRequest.getDefaultInstance();
    when(mockFuture.get()).thenReturn(MutateRowsResponse.getDefaultInstance());
    underTest.mutateRowAsync(request);
    verify(clientCallService).listenableAsyncCall(any(ClientCall.class), same(request));
  }

  @Test
  public void testRetyableCheckAndMutateRow() throws Exception {
    final CheckAndMutateRowRequest request = CheckAndMutateRowRequest.getDefaultInstance();
    when(mockFuture.get()).thenReturn(CheckAndMutateRowResponse.getDefaultInstance());
    underTest.checkAndMutateRow(request);
    verify(clientCallService, times(1)).listenableAsyncCall(any(ClientCall.class), same(request));
  }

  @Test
  public void testRetyableCheckAndMutateRowAsync() {
    CheckAndMutateRowRequest request = CheckAndMutateRowRequest.getDefaultInstance();
    underTest.checkAndMutateRowAsync(request);
    verify(clientCallService).listenableAsyncCall(any(ClientCall.class), same(request));
  }

  @Test
  public void testMutateRowPredicate() {
    Predicate<MutateRowRequest> predicate = BigtableDataGrpcClient.IS_RETRYABLE_MUTATION;
    assertFalse(predicate.apply(null));

    MutateRowRequest.Builder request = MutateRowRequest.newBuilder();
    assertTrue(predicate.apply(request.build()));

    request.addMutations(
        Mutation.newBuilder().setSetCell(SetCell.newBuilder().setTimestampMicros(-1)));
    assertFalse(predicate.apply(request.build()));
  }

  @Test
  public void testMutateRowsPredicate() {
    Predicate<MutateRowsRequest> predicate = BigtableDataGrpcClient.ARE_RETRYABLE_MUTATIONS;
    assertFalse(predicate.apply(null));

    MutateRowsRequest.Builder request = MutateRowsRequest.newBuilder();
    assertTrue(predicate.apply(request.build()));

    request.addEntries(Entry.newBuilder().addMutations(
        Mutation.newBuilder().setSetCell(SetCell.newBuilder().setTimestampMicros(-1))));
    assertFalse(predicate.apply(request.build()));
  }

  @Test
  public void testCheckAndMutateRowPredicate() {
    Predicate<CheckAndMutateRowRequest> predicate =
        BigtableDataGrpcClient.IS_RETRYABLE_CHECK_AND_MUTATE;
    assertFalse(predicate.apply(null));

    CheckAndMutateRowRequest.Builder request = CheckAndMutateRowRequest.newBuilder();
    assertTrue(predicate.apply(request.build()));

    request.addTrueMutations(
        Mutation.newBuilder().setSetCell(SetCell.newBuilder().setTimestampMicros(-1)));
    assertFalse(predicate.apply(request.build()));

    request.clearTrueMutations();
    request.addFalseMutations(
        Mutation.newBuilder().setSetCell(SetCell.newBuilder().setTimestampMicros(-1)));
    assertFalse(predicate.apply(request.build()));
  }

  @Test
  public void testSingleRowRead() {
    ReadRowsRequest request =
        ReadRowsRequest.newBuilder().setRowKey(ByteString.copyFrom(new byte[0])).build();
    underTest.readRows(request);
    verify(channelPool, times(1)).newCall(eq(BigtableServiceGrpc.METHOD_READ_ROWS),
      same(CallOptions.DEFAULT));
  }

  @Test
  public void testMultiRowRead() {
    ReadRowsRequest request =
        ReadRowsRequest.newBuilder().setRowRange(RowRange.getDefaultInstance()).build();
    underTest.readRows(request);
    verify(channelPool, times(1)).newCall(eq(BigtableServiceGrpc.METHOD_READ_ROWS),
      same(CallOptions.DEFAULT));
  }
}
