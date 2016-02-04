
find_path(LibRDKafka_ROOT_DIR
  NAMES include/librdkafka/rdkafkacpp.h
)

find_library(LibRDKafka_LIBRARIES
  NAMES rdkafka++
  HINTS ${LibRDKafka_ROOT_DIR}/lib
)

find_library(LibRDKafka_C_LIBRARIES
	NAMES rdkafka
	HINTS ${LibRDKafka_ROT_DIR}/lib
)

find_path(LibRDKafka_INCLUDE_DIR
  NAMES librdkafka/rdkafkacpp.h
  HINTS ${LibRDKafka_ROOT_DIR}/include
)

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(LibRDKafka DEFAULT_MSG
  LibRDKafka_LIBRARIES
  LibRDKafka_C_LIBRARIES
  LibRDKafka_INCLUDE_DIR
)

mark_as_advanced(
  LibRDKafka_ROOT_DIR
  LibRDKafka_LIBRARIES
  LibRDKafka_C_LIBRARIES
  LibRDKafka_INCLUDE_DIR
)
