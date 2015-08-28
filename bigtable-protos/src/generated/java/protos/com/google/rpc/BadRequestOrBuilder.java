// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/rpc/error_details.proto

package com.google.rpc;

public interface BadRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.rpc.BadRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>repeated .google.rpc.BadRequest.FieldViolation field_violations = 1;</code>
   *
   * <pre>
   * Describes all violations in a client request.
   * </pre>
   */
  java.util.List<com.google.rpc.BadRequest.FieldViolation> 
      getFieldViolationsList();
  /**
   * <code>repeated .google.rpc.BadRequest.FieldViolation field_violations = 1;</code>
   *
   * <pre>
   * Describes all violations in a client request.
   * </pre>
   */
  com.google.rpc.BadRequest.FieldViolation getFieldViolations(int index);
  /**
   * <code>repeated .google.rpc.BadRequest.FieldViolation field_violations = 1;</code>
   *
   * <pre>
   * Describes all violations in a client request.
   * </pre>
   */
  int getFieldViolationsCount();
  /**
   * <code>repeated .google.rpc.BadRequest.FieldViolation field_violations = 1;</code>
   *
   * <pre>
   * Describes all violations in a client request.
   * </pre>
   */
  java.util.List<? extends com.google.rpc.BadRequest.FieldViolationOrBuilder> 
      getFieldViolationsOrBuilderList();
  /**
   * <code>repeated .google.rpc.BadRequest.FieldViolation field_violations = 1;</code>
   *
   * <pre>
   * Describes all violations in a client request.
   * </pre>
   */
  com.google.rpc.BadRequest.FieldViolationOrBuilder getFieldViolationsOrBuilder(
      int index);
}