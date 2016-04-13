#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
# - Try to find openssl include dirs and libraries
#
# Usage of this module as follows:
#
#     find_package(OpenSSL)
#
# Variables used by this module, they can change the default behaviour and need
# to be set before calling find_package:
#
#  OpenSSL_ROOT_DIR          Set this variable to the root installation of
#                            openssl if the module has problems finding the
#                            proper installation path.
#
# Variables defined by this module:
#
#  OPENSSL_FOUND             System has openssl, include and library dirs found
#  OpenSSL_INCLUDE_DIR       The openssl include directories.
#  OpenSSL_LIBRARIES         The openssl libraries.
#  OpenSSL_CYRPTO_LIBRARY    The openssl crypto library.
#  OpenSSL_SSL_LIBRARY       The openssl ssl library.

find_path(OpenSSL_ROOT_DIR
    NAMES include/openssl/ssl.h
)

find_path(OpenSSL_INCLUDE_DIR
    NAMES openssl/ssl.h
    HINTS ${OpenSSL_ROOT_DIR}/include
)

find_library(OpenSSL_SSL_LIBRARY
    NAMES ssl ssleay32 ssleay32MD
    HINTS ${OpenSSL_ROOT_DIR}/lib
)

find_library(OpenSSL_CRYPTO_LIBRARY
    NAMES crypto
    HINTS ${OpenSSL_ROOT_DIR}/lib
)

set(OpenSSL_LIBRARIES ${OpenSSL_SSL_LIBRARY} ${OpenSSL_CRYPTO_LIBRARY}
    CACHE STRING "OpenSSL SSL and crypto libraries" FORCE)

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(OpenSSL DEFAULT_MSG
    OpenSSL_LIBRARIES
    OpenSSL_INCLUDE_DIR
)

mark_as_advanced(
    OpenSSL_ROOT_DIR
    OpenSSL_INCLUDE_DIR
    OpenSSL_LIBRARIES
    OpenSSL_CRYPTO_LIBRARY
    OpenSSL_SSL_LIBRARY
)
