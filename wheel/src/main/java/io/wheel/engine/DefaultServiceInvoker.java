package io.wheel.engine;

import org.apache.curator.x.discovery.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.wheel.ErrorCode;
import io.wheel.ErrorCodeException;
import io.wheel.exceptions.ServiceUndefinedException;
import io.wheel.registry.ServiceDiscovery;
import io.wheel.registry.ServiceImp;
import io.wheel.registry.ServiceInfo;
import io.wheel.registry.ServiceRepository;
import io.wheel.transport.TransportService;
import io.wheel.transport.Transporter;

/**
 * 
 * 
 * @author chuck
 * @since 2014-2-21
 * @version 1.0
 */
public class DefaultServiceInvoker implements ServiceInvoker {

	private static Logger logger = LoggerFactory.getLogger(DefaultServiceInvoker.class);

	private TransportService transportService;

	private ServiceDiscovery serviceDiscovery;

	private ServiceRepository serviceRepository;

	@Override
	public RpcResponse invoke(RpcRequest request) {
		String serviceCode = request.getServiceCode();
		ServiceImp serviceImp = serviceRepository.getServiceImp(serviceCode);
		if (serviceImp == null) {
			throw new ServiceUndefinedException(serviceCode);
		}
		RpcResponse response = null;
		try {
			String registry = serviceImp.getRegistry();
			String serviceGroup = serviceImp.getServiceGroup();
			ServiceProvider<ServiceInfo> provider = serviceDiscovery.getServiceProvider(registry, serviceGroup);
			Transporter transporter = transportService.getTransporter(serviceImp.getProtocol());
			response = transporter.invoke(provider, request);
		} catch (Exception e) {
			logger.error("Call remote service error!serviceCode={},method name={}", e);
			throw new ErrorCodeException(ErrorCode.SERVICE_INVOKE_ERROR, e);
		}

		if (response == null) {
			throw new ErrorCodeException(ErrorCode.SERVICE_INVOKE_ERROR);
		}

		if (!response.isSuccess()) {
			ErrorCodeException e = new ErrorCodeException();
			e.setErrorCode(response.getResultCode());
			e.setErrorMessage(response.getResultMessage());
			throw e;
		}
		return response;

	}

	public void setServiceDiscovery(ServiceDiscovery serviceDiscovery) {
		this.serviceDiscovery = serviceDiscovery;
	}

	public void setServiceRepository(ServiceRepository serviceRepository) {
		this.serviceRepository = serviceRepository;
	}

	public void setTransportService(TransportService transportService) {
		this.transportService = transportService;
	}

}
