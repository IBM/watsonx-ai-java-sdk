# IBM watsonx.ai Java SDK

IBM watsonx.ai Java SDK is a client library that interacts with the [IBM watsonx.ai APIs](https://cloud.ibm.com/apidocs/watsonx-ai).

## Features

This Java SDK provides a convenient abstraction over the HTTP APIs offered by `watsonx.ai`, simplifying common tasks such as request construction and response handling. It helps developers integrate with the service more easily, so they can focus on building their applications.

## Prerequisites

- A [watsonx.ai](https://www.ibm.com/watsonx/get-started) service instance.
- Java 17 or higher.

## Samples

Examples of usage are available in the [samples/](samples) directory.

## Getting Started

To use the watsonx.ai Java SDK, add the following Maven dependency:

```xml
<dependency>
    <groupId>com.ibm.watsonx</groupId>
    <artifactId>watsonx-ai</artifactId>
    <version>0.15.0</version>
</dependency>
```

## Contributing

We welcome contributions to this project!

To ensure a smooth contribution process, please follow these guidelines:

- All source files must include a license header using the SPDX format for the Apache License 2.0:

  ```
  /*
   * Copyright <holder> All Rights Reserved.
   * SPDX-License-Identifier: Apache-2.0
   */
  ```
  This header, along with code formatting, is automatically applied to source files using the following command:

  ```
  ./mvnw compile
  ```

- When submitting a patch, you must include a Signed-off-by line in your commit message,
  indicating that you agree to the DCO:

  ```
  Signed-off-by: Your Name <your.email@example.com>
  ```

  You can automatically add this line by using the `-s` flag with git commit:

  ```
  git commit -s
  ```

Feel free to submit issues and pull requests. We appreciate your contributions!

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

